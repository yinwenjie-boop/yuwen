package com.zhongkao.yuwen.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * App 与 DeepSeek 之间的 JSON 契约（对应 SPEC v2 §6）。
 * 线上格式统一用 snake_case；提示词必须要求模型严格按这些 key 输出。
 * 解析时配合 Json { ignoreUnknownKeys = true; coerceInputValues = true }。
 */

// ---------- 出题返回 ----------

@Serializable
data class GeneratedExercise(
    @SerialName("type") val type: String,                 // wenyan_compare / wenyan_single / xiandai
    @SerialName("total_score") val totalScore: Int,
    @SerialName("passage_in") val passageIn: Passage? = null,   // 文言对比：甲（课内）
    @SerialName("passage_out") val passageOut: Passage? = null, // 文言对比：乙（课外）
    @SerialName("passage") val passage: Passage? = null,        // 文言单篇 / 现代文
    @SerialName("questions") val questions: List<GenQuestion> = emptyList()
)

@Serializable
data class Passage(
    @SerialName("title") val title: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("dynasty") val dynasty: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("notes") val notes: List<Note> = emptyList(),
    @SerialName("source_ref") val sourceRef: String = "",       // 出处：书名+朝代+作者
    @SerialName("verified") val verified: Boolean = true,       // 是否确认真实
    @SerialName("need_human_review") val needHumanReview: Boolean = false
)

@Serializable
data class Note(
    @SerialName("term") val term: String,
    @SerialName("explain") val explain: String
)

@Serializable
data class GenQuestion(
    @SerialName("id") val id: String,
    @SerialName("q_type") val qType: String,                    // 实词解释/虚词辨析/断句/句子翻译/内容理解...
    @SerialName("stem") val stem: String,
    @SerialName("ref_answer") val refAnswer: String,
    @SerialName("score_points") val scorePoints: List<ScorePoint> = emptyList(),
    @SerialName("max_score") val maxScore: Int,
    @SerialName("ability_tag") val abilityTag: String           // 用于薄弱点统计
)

@Serializable
data class ScorePoint(
    @SerialName("point") val point: String,
    @SerialName("score") val score: Int
)

// ---------- 批改返回（AI 部分） ----------

@Serializable
data class GradingResult(
    @SerialName("per_question") val perQuestion: List<QuestionGrade> = emptyList(),
    @SerialName("total_got") val totalGot: Int,
    @SerialName("total_full") val totalFull: Int,
    @SerialName("weak_points") val weakPoints: List<String> = emptyList(),
    @SerialName("next_advice") val nextAdvice: String = ""
)

@Serializable
data class QuestionGrade(
    @SerialName("id") val id: String,
    @SerialName("got_score") val gotScore: Int,
    @SerialName("max_score") val maxScore: Int,
    @SerialName("point_check") val pointCheck: List<PointCheck> = emptyList(),
    @SerialName("error_type") val errorType: String = "",
    @SerialName("correct_answer") val correctAnswer: String = "",
    @SerialName("explanation") val explanation: String = "",
    @SerialName("tip") val tip: String = ""
)

@Serializable
data class PointCheck(
    @SerialName("point") val point: String,
    @SerialName("hit") val hit: Boolean,
    @SerialName("score") val score: Int
)

// ---------- 本次结算（App 本地合成，非 AI 返回；可入库/展示） ----------

@Serializable
data class ExerciseSettlement(
    @SerialName("time_spent_sec") val timeSpentSec: Int,
    @SerialName("baseline_range") val baselineRange: List<Int>, // [下限秒, 上限秒]
    @SerialName("per_question_time") val perQuestionTime: List<QuestionTime> = emptyList(),
    @SerialName("slowest_question") val slowestQuestion: QuestionTime? = null,
    @SerialName("time_rating") val timeRating: String,          // 见 TimeEvaluator.Quadrant.label
    @SerialName("accuracy") val accuracy: Double,
    @SerialName("xp_gained") val xpGained: Int,
    @SerialName("coins_gained") val coinsGained: Int,
    @SerialName("badge_earned") val badgeEarned: BadgeAward? = null
)

@Serializable
data class QuestionTime(
    @SerialName("id") val id: String,
    @SerialName("sec") val sec: Int
)

@Serializable
data class BadgeAward(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String
)
