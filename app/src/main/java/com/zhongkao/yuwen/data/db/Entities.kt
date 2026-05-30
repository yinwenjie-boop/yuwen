package com.zhongkao.yuwen.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体（对应 SPEC v2 §4）。
 *
 * 约定：
 *  - 形如 notes / scorePoints / pointCheck / configJson / payloadJson 的"JSON 字段"
 *    统一以原始 JSON 字符串入库（出题/批改快照），读出时再用 AppJson 解析为 DTO。
 *  - 时间戳用 epoch 毫秒(Long)；纯日期(如最近练习日)用 ISO-8601 字符串(yyyy-MM-dd)。
 */

/** 文言语料库：课内 22 篇为固定语料（verified=true），课外篇按可靠性入库。 */
@Entity(tableName = "text_bank")
data class TextBank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,                 // 课内 / 课外
    val title: String,
    val author: String = "",
    val dynasty: String = "",
    val text: String,
    val notes: String = "[]",             // JSON: List<Note>
    val translation: String = "",
    val sourceRef: String = "",           // 出处：书名+朝代+作者
    val theme: String = "",
    val verified: Boolean = false         // 课内固定语料=true；AI 提议课外篇=false 待人工确认
)

/** 一次练习（出题快照 + 本次总用时）。 */
@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                     // wenyan_compare / wenyan_single / xiandai
    val createdAt: Long,
    val configJson: String = "{}",        // 出题设置快照
    val payloadJson: String = "{}",       // 出题返回快照(GeneratedExercise)
    val status: String = "draft",         // draft / answering / graded
    val totalTimeSec: Int = 0             // 本次总用时
)

/** 单题（属于某次练习）。 */
@Entity(
    tableName = "question",
    indices = [Index("exerciseId")]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val qType: String,
    val stem: String,
    val refAnswer: String = "",
    val scorePoints: String = "[]",       // JSON: List<ScorePoint>
    val maxScore: Int,
    val abilityTag: String = ""           // 用于薄弱点统计
)

/** 一次作答 + 批改结果 + 本题用时。 */
@Entity(
    tableName = "attempt",
    indices = [Index("questionId")]
)
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val studentAnswer: String = "",
    val gotScore: Int = 0,
    val pointCheck: String = "[]",        // JSON: List<PointCheck>
    val errorType: String = "",
    val explanation: String = "",
    val tip: String = "",
    val timeSpentSec: Int = 0,            // 本题用时
    val gradedAt: Long = 0
)

/** 错题本条目。 */
@Entity(
    tableName = "wrong_question",
    indices = [Index("questionId")]
)
data class WrongQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val abilityTag: String = "",
    val addedAt: Long,
    val masteredFlag: Boolean = false
)

/** 各考点统计（含平均用时，用于定位"又慢又错"）。abilityTag 作主键。 */
@Entity(tableName = "weak_point_stat")
data class WeakPointStat(
    @PrimaryKey val abilityTag: String,
    val attempts: Int = 0,
    val wrongCount: Int = 0,
    val avgTimeSec: Int = 0,
    val lastUpdated: Long = 0
)

/** 用户进度（单行，id 固定为 1）。 */
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val xp: Int = 0,
    val level: Int = 1,
    val levelTitle: String = "文言学徒",
    val coins: Int = 0,
    val streakDays: Int = 0,
    val lastPracticeDate: String? = null  // ISO yyyy-MM-dd
) {
    companion object { const val SINGLETON_ID = 1 }
}

/** 徽章（§7.3 四象限 / 里程碑）。 */
@Entity(tableName = "badge")
data class Badge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,                     // fast_accurate / slow_accurate ... 或里程碑码
    val name: String,
    val exerciseId: Long? = null,
    val earnedAt: Long
)
