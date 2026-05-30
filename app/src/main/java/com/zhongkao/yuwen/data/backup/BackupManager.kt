package com.zhongkao.yuwen.data.backup

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.db.Attempt
import com.zhongkao.yuwen.data.db.Badge
import com.zhongkao.yuwen.data.db.Exercise
import com.zhongkao.yuwen.data.db.Question
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.data.db.WeakPointStat
import com.zhongkao.yuwen.data.db.WrongQuestion

/**
 * 备份/恢复引擎（阶段 6）。纯编排逻辑，数据存取经 [BackupStore] 抽象，
 * 既可由 Room 实现 ([RoomBackupStore])，也可在单测里注入内存假实现。
 *
 * 安全：导出内容只含错题本/薄弱点/进度/徽章，绝不含 API Key。
 * 导入语义：薄弱点与进度按主键 upsert（覆盖）；错题与徽章为追加，
 *           建议在「干净安装」上导入恢复（重复导入会叠加错题）。
 */
class BackupManager(
    private val store: BackupStore,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /** 汇总当前数据为备份 JSON 字符串。 */
    suspend fun exportToJson(): String {
        val progress = store.loadProgress()
        val wrongs = store.loadWrongs().mapNotNull { w ->
            val q = store.loadQuestion(w.questionId) ?: return@mapNotNull null // 题目缺失则跳过，保证自包含
            val ex = store.loadExercise(q.exerciseId)
            val attempt = store.loadLatestAttempt(w.questionId)
            WrongBackup(
                abilityTag = w.abilityTag,
                addedAt = w.addedAt,
                mastered = w.masteredFlag,
                exerciseType = ex?.type ?: "",
                question = QuestionBackup(
                    genId = q.genId, qType = q.qType, stem = q.stem, refAnswer = q.refAnswer,
                    scorePoints = q.scorePoints, maxScore = q.maxScore, abilityTag = q.abilityTag
                ),
                attempt = attempt?.let {
                    AttemptBackup(
                        studentAnswer = it.studentAnswer, gotScore = it.gotScore, pointCheck = it.pointCheck,
                        errorType = it.errorType, correctAnswer = it.correctAnswer, explanation = it.explanation,
                        tip = it.tip, timeSpentSec = it.timeSpentSec, gradedAt = it.gradedAt
                    )
                }
            )
        }
        val file = BackupFile(
            version = 1,
            exportedAt = now(),
            userProgress = progress?.let {
                ProgressBackup(it.xp, it.level, it.levelTitle, it.coins, it.streakDays, it.lastPracticeDate)
            },
            weakPoints = store.loadWeakPoints().map {
                WeakPointBackup(it.abilityTag, it.attempts, it.wrongCount, it.avgTimeSec, it.lastUpdated)
            },
            badges = store.loadBadges().map { BadgeBackup(it.code, it.name, it.earnedAt) },
            wrongQuestions = wrongs
        )
        return AppJson.encodeToString(BackupFile.serializer(), file)
    }

    /** 解析并恢复备份；解析失败抛 [IllegalArgumentException]，调用方需捕获并提示（不改动现有数据）。 */
    suspend fun importFromJson(json: String): ImportSummary {
        val file = try {
            AppJson.decodeFromString(BackupFile.serializer(), json)
        } catch (e: Exception) {
            throw IllegalArgumentException("不是有效的备份文件：${e.message ?: "格式错误"}")
        }

        var progressRestored = false
        file.userProgress?.let { p ->
            store.saveProgress(
                UserProgress(
                    id = UserProgress.SINGLETON_ID,
                    xp = p.xp, level = p.level, levelTitle = p.levelTitle,
                    coins = p.coins, streakDays = p.streakDays, lastPracticeDate = p.lastPracticeDate
                )
            )
            progressRestored = true
        }

        file.weakPoints.forEach {
            store.upsertWeakPoint(
                WeakPointStat(it.abilityTag, it.attempts, it.wrongCount, it.avgTimeSec, it.lastUpdated)
            )
        }

        file.badges.forEach { store.insertBadge(Badge(code = it.code, name = it.name, earnedAt = it.earnedAt)) }

        var wrongCount = 0
        file.wrongQuestions.forEach { wb ->
            val q = wb.question
            // 重建「练习→题目→作答」骨架，使错题本 JOIN 查询导入后即可展示。
            // status=restored：不计入「近 N 次用时曲线」(只取 status=graded)，避免污染时间统计。
            val exerciseId = store.insertExercise(
                Exercise(
                    type = wb.exerciseType.ifBlank { "xiandai" },
                    createdAt = wb.addedAt,
                    status = STATUS_RESTORED
                )
            )
            val questionId = store.insertQuestion(
                Question(
                    exerciseId = exerciseId, genId = q.genId, qType = q.qType, stem = q.stem,
                    refAnswer = q.refAnswer, scorePoints = q.scorePoints, maxScore = q.maxScore,
                    abilityTag = q.abilityTag
                )
            )
            wb.attempt?.let { a ->
                store.insertAttempt(
                    Attempt(
                        questionId = questionId, studentAnswer = a.studentAnswer, gotScore = a.gotScore,
                        pointCheck = a.pointCheck, errorType = a.errorType, correctAnswer = a.correctAnswer,
                        explanation = a.explanation, tip = a.tip, timeSpentSec = a.timeSpentSec, gradedAt = a.gradedAt
                    )
                )
            }
            store.insertWrong(
                WrongQuestion(
                    questionId = questionId, abilityTag = wb.abilityTag,
                    addedAt = wb.addedAt, masteredFlag = wb.mastered
                )
            )
            wrongCount++
        }

        return ImportSummary(
            wrongQuestions = wrongCount,
            weakPoints = file.weakPoints.size,
            badges = file.badges.size,
            progressRestored = progressRestored
        )
    }

    companion object {
        const val STATUS_RESTORED = "restored"
    }
}
