package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.data.ai.ExerciseSettlement
import com.zhongkao.yuwen.data.ai.GradingResult
import com.zhongkao.yuwen.data.ai.QuestionTime
import com.zhongkao.yuwen.domain.ExamConfig
import com.zhongkao.yuwen.domain.TimeEvaluator
import com.zhongkao.yuwen.domain.incentive.BadgeRules

/**
 * 本次结算合成（SPEC §6/§7）：用 ExamConfig 的每分值基准 + TimeEvaluator 四象限，
 * 把"用时 × 正确率"合成 ExerciseSettlement。XP/积分由阶段 4 激励算好后传入展示；
 * 徽章名按四象限取（与发章一致）。
 */
object SettlementCalculator {

    /** 把出题题型 + 现代文文体映射到 exam_config.json 的时间基准键。 */
    fun baselineKey(apiType: String, genre: String?): String = when (apiType) {
        ExerciseType.WENYAN_COMPARE.apiType -> ExamConfig.KEY_WENYAN_COMPARE
        ExerciseType.XIANDAI.apiType ->
            if (genre == "说明文" || genre == "议论文") ExamConfig.KEY_XIANDAI_EXPOSITORY
            else ExamConfig.KEY_XIANDAI_NARRATIVE
        "wenyan_single" -> ExamConfig.KEY_WENYAN_SINGLE
        else -> ExamConfig.KEY_XIANDAI_NARRATIVE
    }

    fun accuracy(grading: GradingResult): Double =
        if (grading.totalFull > 0) grading.totalGot.toDouble() / grading.totalFull else 0.0

    /**
     * @param perQuestionSec  genId -> 本题用时（秒）
     */
    fun build(
        apiType: String,
        genre: String?,
        totalScore: Int,
        totalTimeSec: Int,
        perQuestionSec: Map<String, Int>,
        grading: GradingResult,
        examConfig: ExamConfig,
        xpGained: Int = 0,
        coinsGained: Int = 0
    ): ExerciseSettlement {
        val acc = accuracy(grading)
        val baseline = examConfig.baselineRange(baselineKey(apiType, genre), totalScore)
        val rating = if (baseline != null) {
            TimeEvaluator.evaluate(totalTimeSec, baseline, acc, examConfig.accuracyThreshold).label
        } else {
            "（缺少时间基准）"
        }
        val perQuestionTime = perQuestionSec.map { (id, sec) -> QuestionTime(id, sec) }
        val slowest = perQuestionTime.maxByOrNull { it.sec }
        val quadrantCode = if (baseline != null) {
            TimeEvaluator.evaluate(totalTimeSec, baseline, acc, examConfig.accuracyThreshold).code
        } else null

        return ExerciseSettlement(
            timeSpentSec = totalTimeSec,
            baselineRange = baseline?.let { listOf(it.first, it.last) } ?: listOf(0, 0),
            perQuestionTime = perQuestionTime,
            slowestQuestion = slowest,
            timeRating = rating,
            accuracy = acc,
            xpGained = xpGained,
            coinsGained = coinsGained,
            badgeEarned = BadgeRules.quadrantBadge(quadrantCode)
        )
    }
}
