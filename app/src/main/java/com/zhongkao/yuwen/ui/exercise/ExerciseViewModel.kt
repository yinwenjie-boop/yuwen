package com.zhongkao.yuwen.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.ai.GeneratedExercise
import com.zhongkao.yuwen.data.ai.ScorePoint
import com.zhongkao.yuwen.data.db.Attempt
import com.zhongkao.yuwen.data.db.Exercise
import com.zhongkao.yuwen.data.db.Question
import com.zhongkao.yuwen.domain.timer.ExerciseTimer
import com.zhongkao.yuwen.domain.usecase.GenerateExerciseUseCase
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

sealed interface ExerciseUiState {
    data object Loading : ExerciseUiState
    data class Failed(val message: String) : ExerciseUiState
    data class Invalid(val errors: List<String>) : ExerciseUiState
    data class NeedsReview(val warnings: List<String>) : ExerciseUiState
    data class Answering(val generated: GeneratedExercise) : ExerciseUiState
    data class Submitted(val totalSec: Int, val exerciseId: Long) : ExerciseUiState
}

/**
 * 答题页 VM（阶段 2）：触发出题 → 落库 → 计时作答 → 提交记录每题用时。
 * 批改与结果在阶段 3 接入。
 */
class ExerciseViewModel(private val container: AppContainer) : ViewModel() {

    private val timer = ExerciseTimer()

    private val _state = MutableStateFlow<ExerciseUiState>(ExerciseUiState.Loading)
    val state: StateFlow<ExerciseUiState> = _state.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private var exerciseId: Long = 0
    private var genToQuestionId: Map<String, Long> = emptyMap()
    private var generated: GeneratedExercise? = null

    init { start() }

    fun start() {
        val req = container.pendingRequest
        if (req == null) {
            _state.value = ExerciseUiState.Failed("没有出题请求，请返回重新设置。")
            return
        }
        _state.value = ExerciseUiState.Loading
        viewModelScope.launch {
            when (val r = container.generateExerciseUseCase.generate(req)) {
                is GenerateExerciseUseCase.Result.Failure -> _state.value = ExerciseUiState.Failed(r.message)
                is GenerateExerciseUseCase.Result.Invalid -> _state.value = ExerciseUiState.Invalid(r.errors)
                is GenerateExerciseUseCase.Result.NeedsReview -> _state.value = ExerciseUiState.NeedsReview(r.warnings)
                is GenerateExerciseUseCase.Result.Success -> persistAndStart(req, r)
            }
        }
    }

    private suspend fun persistAndStart(req: GenerationRequest, success: GenerateExerciseUseCase.Result.Success) {
        val gen = success.generated
        generated = gen

        val exercise = Exercise(
            type = req.type.apiType,
            createdAt = System.currentTimeMillis(),
            configJson = AppJson.encodeToString(GenerationRequest.serializer(), req),
            payloadJson = success.snapshotJson,
            status = STATUS_ANSWERING,
            totalTimeSec = 0
        )
        val questions = gen.questions.map { q ->
            Question(
                exerciseId = 0,
                genId = q.id,
                qType = q.qType,
                stem = q.stem,
                refAnswer = q.refAnswer,
                scorePoints = AppJson.encodeToString(ListSerializer(ScorePoint.serializer()), q.scorePoints),
                maxScore = q.maxScore,
                abilityTag = q.abilityTag
            )
        }
        val saved = container.exerciseRepository.saveGenerated(exercise, questions)
        exerciseId = saved.exerciseId
        genToQuestionId = gen.questions.mapIndexed { i, q -> q.id to saved.questionIds[i] }.toMap()

        _answers.value = gen.questions.associate { it.id to "" }
        _index.value = 0
        _state.value = ExerciseUiState.Answering(gen)
        gen.questions.firstOrNull()?.let { timer.start(it.id) }
    }

    fun onAnswerChange(genId: String, text: String) {
        _answers.update { it + (genId to text) }
    }

    fun goTo(i: Int) {
        val gen = generated ?: return
        if (i in gen.questions.indices) {
            timer.switchTo(gen.questions[i].id)
            _index.value = i
        }
    }

    /** 切后台暂停 / 回前台恢复（由界面的生命周期回调驱动）。 */
    fun pauseTimer() = timer.pause()
    fun resumeTimer() = timer.resume()

    fun submit() {
        val gen = generated ?: return
        viewModelScope.launch {
            val snap = timer.stop()
            val secByGen = snap.perQuestion.associate { it.id to it.sec }
            val attempts = gen.questions.mapNotNull { q ->
                val qid = genToQuestionId[q.id] ?: return@mapNotNull null
                Attempt(
                    questionId = qid,
                    studentAnswer = _answers.value[q.id].orEmpty(),
                    gotScore = 0,
                    timeSpentSec = secByGen[q.id] ?: 0,
                    gradedAt = 0
                )
            }
            container.exerciseRepository.saveAttempts(attempts)
            container.exerciseRepository.findExercise(exerciseId)?.let {
                container.exerciseRepository.updateExercise(
                    it.copy(totalTimeSec = snap.totalSec, status = STATUS_SUBMITTED)
                )
            }
            _state.value = ExerciseUiState.Submitted(snap.totalSec, exerciseId)
        }
    }

    companion object {
        const val STATUS_ANSWERING = "answering"
        const val STATUS_SUBMITTED = "submitted"
    }
}
