package com.zhongkao.yuwen.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 本地备份文件结构（阶段 6）。
 * 导出错题本、薄弱点、UserProgress（及徽章）为单个 JSON，可在重装/换机后导入恢复。
 * 安全约束：绝不包含 API Key（Key 只存 EncryptedSharedPreferences，不进备份）。
 *
 * 错题为「自包含」备份：连同题目与本次作答快照一起导出，导入后无需原练习即可在错题本展示。
 */
@Serializable
data class BackupFile(
    val version: Int = 1,
    @SerialName("exported_at") val exportedAt: Long = 0,
    @SerialName("user_progress") val userProgress: ProgressBackup? = null,
    @SerialName("weak_points") val weakPoints: List<WeakPointBackup> = emptyList(),
    val badges: List<BadgeBackup> = emptyList(),
    @SerialName("wrong_questions") val wrongQuestions: List<WrongBackup> = emptyList()
)

@Serializable
data class ProgressBackup(
    val xp: Int = 0,
    val level: Int = 1,
    @SerialName("level_title") val levelTitle: String = "文言学徒",
    val coins: Int = 0,
    @SerialName("streak_days") val streakDays: Int = 0,
    @SerialName("last_practice_date") val lastPracticeDate: String? = null
)

@Serializable
data class WeakPointBackup(
    @SerialName("ability_tag") val abilityTag: String,
    val attempts: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    @SerialName("avg_time_sec") val avgTimeSec: Int = 0,
    @SerialName("last_updated") val lastUpdated: Long = 0
)

@Serializable
data class BadgeBackup(
    val code: String,
    val name: String,
    @SerialName("earned_at") val earnedAt: Long = 0
)

@Serializable
data class WrongBackup(
    @SerialName("ability_tag") val abilityTag: String = "",
    @SerialName("added_at") val addedAt: Long = 0,
    val mastered: Boolean = false,
    @SerialName("exercise_type") val exerciseType: String = "",
    val question: QuestionBackup,
    val attempt: AttemptBackup? = null
)

@Serializable
data class QuestionBackup(
    @SerialName("gen_id") val genId: String = "",
    @SerialName("q_type") val qType: String = "",
    val stem: String = "",
    @SerialName("ref_answer") val refAnswer: String = "",
    @SerialName("score_points") val scorePoints: String = "[]",
    @SerialName("max_score") val maxScore: Int = 0,
    @SerialName("ability_tag") val abilityTag: String = ""
)

@Serializable
data class AttemptBackup(
    @SerialName("student_answer") val studentAnswer: String = "",
    @SerialName("got_score") val gotScore: Int = 0,
    @SerialName("point_check") val pointCheck: String = "[]",
    @SerialName("error_type") val errorType: String = "",
    @SerialName("correct_answer") val correctAnswer: String = "",
    val explanation: String = "",
    val tip: String = "",
    @SerialName("time_spent_sec") val timeSpentSec: Int = 0,
    @SerialName("graded_at") val gradedAt: Long = 0
)

/** 导入结果汇总（用于给用户的反馈文案）。 */
data class ImportSummary(
    val wrongQuestions: Int,
    val weakPoints: Int,
    val badges: Int,
    val progressRestored: Boolean
)
