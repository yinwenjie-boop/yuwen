package com.zhongkao.yuwen.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.ai.ExerciseSettlement
import com.zhongkao.yuwen.data.ai.GeneratedExercise
import com.zhongkao.yuwen.data.ai.GradingResult
import com.zhongkao.yuwen.data.ai.PointCheck
import com.zhongkao.yuwen.data.ai.QuestionGrade
import com.zhongkao.yuwen.data.db.Question
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.domain.TimeEvaluator
import com.zhongkao.yuwen.domain.incentive.LevelSystem
import com.zhongkao.yuwen.domain.usecase.GradeExerciseUseCase
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import com.zhongkao.yuwen.domain.usecase.RecordResultsUseCase
import com.zhongkao.yuwen.domain.usecase.SettlementCalculator
import com.zhongkao.yuwen.domain.usecase.StudentAnswerDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

data class GradedItem(
    val genId: String,
    val qType: String,
    val stem: String,
    val maxScore: Int,
    val gotScore: Int,
    val studentAnswer: String,
    val pointChecks: List<PointCheck>,
    val errorType: String,
    val correctAnswer: String,
    val explanation: String,
    val tip: String,
    val timeSec: Int
)

/** 即时结算 / 进度展示（练后激励四件套之"结算动画"数据）。 */
data class RewardView(
    val xpGained: Int,
    val coinsGained: Int,
    val totalXp: Int,
    val totalCoins: Int,
    val level: Int,
    val levelTitle: String,
    val xpIntoLevel: Int,
    val xpPerLevel: Int,
    val leveledUp: Boolean,
    val newBadges: List<String>,
    val streakDays: Int,
    val freshlyGraded: Boolean       // 首次批改才真正发放，重看结果只展示当前进度
)

data class ResultView(
    val items: List<GradedItem>,
    val totalGot: Int,
    val totalFull: Int,
    val weakPoints: List<String>,
    val nextAdvice: String,
    val settlement: ExerciseSettlement,
    val reward: RewardView
)

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Failed(val message: String) : ResultUiState
    data class Content(val view: ResultView) : ResultUiState
}

/**
 * 结果页 VM（阶段 3）：若未批改则调批改用例并写回 Attempt/Exercise；已批改则直接读快照。
 * 合成时间评价（§7 四象限）。激励(XP/积分/徽章)在阶段 4 接入。
 */
class ResultViewModel(
    private val container: AppContainer,
    private val exerciseId: Long
) : ViewModel() {

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = ResultUiState.Loading
        viewModelScope.launch { runLoad() }
    }

    private suspend fun runLoad() {
        val repo = container.exerciseRepository
        val exercise = repo.findExercise(exerciseId)
            ?: run { _state.value = ResultUiState.Failed("找不到本次练习记录。"); return }

        val gen = runCatching {
            AppJson.decodeFromString(GeneratedExercise.serializer(), exercise.payloadJson)
        }.getOrNull() ?: run { _state.value = ResultUiState.Failed("出题快照损坏，无法批改。"); return }

        val genre = runCatching {
            AppJson.decodeFromString(GenerationRequest.serializer(), exercise.configJson).genre
        }.getOrNull()

        // genId -> Question 行（取 questionId）；genId -> 最近一次作答
        val questionRows: List<Question> = repo.questionsOf(exerciseId)
        val rowByGen = questionRows.associateBy { it.genId }
        val attemptByGen = questionRows.associate { row ->
            row.genId to repo.latestAttempt(row.id)
        }

        // 取批改结果：已批改读快照；否则现批改并写回。
        var freshlyGraded = false
        val grading: GradingResult = if (exercise.status == "graded" && exercise.gradingJson.isNotBlank()) {
            runCatching { AppJson.decodeFromString(GradingResult.serializer(), exercise.gradingJson) }
                .getOrNull() ?: run { _state.value = ResultUiState.Failed("批改快照损坏，请重试。"); return }
        } else {
            val answers = gen.questions.map { q ->
                StudentAnswerDto(q.id, attemptByGen[q.id]?.studentAnswer.orEmpty())
            }
            when (val r = container.gradeExerciseUseCase.grade(
                questions = gen.questions,
                answers = answers,
                expectedIds = gen.questions.map { it.id }.toSet()
            )) {
                is GradeExerciseUseCase.Result.Failure -> {
                    _state.value = ResultUiState.Failed(r.message); return
                }
                is GradeExerciseUseCase.Result.Invalid -> {
                    _state.value = ResultUiState.Failed("批改校验未通过：\n" + r.errors.joinToString("\n") { "· $it" }); return
                }
                is GradeExerciseUseCase.Result.Success -> {
                    writeBack(r.grading, rowByGen)
                    container.exerciseRepository.updateExercise(
                        exercise.copy(
                            gradingJson = AppJson.encodeToString(GradingResult.serializer(), r.grading),
                            status = "graded"
                        )
                    )
                    freshlyGraded = true
                    r.grading
                }
            }
        }

        val perQuestionSec = gen.questions.associate { it.id to (attemptByGen[it.id]?.timeSpentSec ?: 0) }
        val gradeByGen = grading.perQuestion.associateBy { it.id }

        // 先算一遍结算以拿到本次象限/是否达标。
        val baseSettlement = SettlementCalculator.build(
            apiType = exercise.type,
            genre = genre,
            totalScore = gen.totalScore,
            totalTimeSec = exercise.totalTimeSec,
            perQuestionSec = perQuestionSec,
            grading = grading,
            examConfig = container.examConfig
        )
        val quadrantCode = baseSettlement.badgeEarned?.code
        val targetMet = quadrantCode == TimeEvaluator.Quadrant.FAST_ACCURATE.code

        // 首次批改：错题入本 + 薄弱点滚动 + 发激励（只发一次）。
        val freshReward: RecordResultsUseCase.Reward? = if (freshlyGraded) {
            val outcomes = questionRows.mapNotNull { row ->
                val g = gradeByGen[row.genId] ?: return@mapNotNull null
                RecordResultsUseCase.QuestionOutcome(
                    questionRoomId = row.id,
                    abilityTag = row.abilityTag,
                    gotScore = g.gotScore,
                    maxScore = row.maxScore,
                    timeSec = perQuestionSec[row.genId] ?: 0
                )
            }
            container.recordResultsUseCase.record(
                questions = outcomes,
                totalGot = grading.totalGot,
                quadrantCode = quadrantCode,
                targetMet = targetMet,
                exerciseId = exerciseId
            )
        } else null

        val settlement = baseSettlement.copy(
            xpGained = freshReward?.xpGained ?: 0,
            coinsGained = freshReward?.coinsGained ?: 0
        )

        // 进度展示：用发放后的最新进度（重看结果则读当前进度）。
        val config = container.incentiveSettingsStore.getConfig()
        val progress: UserProgress = freshReward?.progress
            ?: container.progressRepository.getProgress()
            ?: UserProgress()
        val reward = RewardView(
            xpGained = freshReward?.xpGained ?: 0,
            coinsGained = freshReward?.coinsGained ?: 0,
            totalXp = progress.xp,
            totalCoins = progress.coins,
            level = progress.level,
            levelTitle = progress.levelTitle,
            xpIntoLevel = LevelSystem.xpIntoLevel(progress.xp, config.xpPerLevel),
            xpPerLevel = config.xpPerLevel,
            leveledUp = freshReward?.leveledUp ?: false,
            newBadges = freshReward?.newBadges?.map { it.name } ?: emptyList(),
            streakDays = progress.streakDays,
            freshlyGraded = freshlyGraded
        )

        val items = gen.questions.map { q ->
            val g = gradeByGen[q.id]
            GradedItem(
                genId = q.id,
                qType = q.qType,
                stem = q.stem,
                maxScore = q.maxScore,
                gotScore = g?.gotScore ?: 0,
                studentAnswer = attemptByGen[q.id]?.studentAnswer.orEmpty(),
                pointChecks = g?.pointCheck ?: emptyList(),
                errorType = g?.errorType.orEmpty(),
                correctAnswer = g?.correctAnswer.orEmpty().ifBlank { q.refAnswer },
                explanation = g?.explanation.orEmpty(),
                tip = g?.tip.orEmpty(),
                timeSec = perQuestionSec[q.id] ?: 0
            )
        }

        _state.value = ResultUiState.Content(
            ResultView(
                items = items,
                totalGot = grading.totalGot,
                totalFull = grading.totalFull,
                weakPoints = grading.weakPoints,
                nextAdvice = grading.nextAdvice,
                settlement = settlement,
                reward = reward
            )
        )
    }

    /** 把批改结果写回 Attempt（供阶段 4 错题本/薄弱点统计）。 */
    private suspend fun writeBack(grading: GradingResult, rowByGen: Map<String, Question>) {
        val now = System.currentTimeMillis()
        val gradeByGen: Map<String, QuestionGrade> = grading.perQuestion.associateBy { it.id }
        for ((genId, row) in rowByGen) {
            val g = gradeByGen[genId] ?: continue
            val attempt = container.exerciseRepository.latestAttempt(row.id) ?: continue
            container.exerciseRepository.updateAttempt(
                attempt.copy(
                    gotScore = g.gotScore,
                    pointCheck = AppJson.encodeToString(ListSerializer(PointCheck.serializer()), g.pointCheck),
                    errorType = g.errorType,
                    correctAnswer = g.correctAnswer,
                    explanation = g.explanation,
                    tip = g.tip,
                    gradedAt = now
                )
            )
        }
    }
}
