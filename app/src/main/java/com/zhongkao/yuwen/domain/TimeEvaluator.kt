package com.zhongkao.yuwen.domain

/**
 * 时间评价（对应 SPEC v2 §7）。
 * 基准来自 ExamConfig（exam_config.json），按本次实际总分线性归一化，
 * 再用「用时是否达标 × 正确率是否达标」给四象限评语。
 */
object TimeEvaluator {

    enum class Quadrant(val code: String, val label: String) {
        FAST_ACCURATE("fast_accurate", "考场节奏 · 又快又准"),
        SLOW_ACCURATE("slow_accurate", "准度够 · 该提速"),
        FAST_INACCURATE("fast_inaccurate", "求快丢分 · 放慢审题"),
        SLOW_INACCURATE("slow_inaccurate", "基础待巩固 · 先求对再求快")
    }

    /**
     * @param baselineSecPerScore 该题型「每分值基准秒数」区间（来自配置）
     * @param totalScore          本次实际生成的总分
     * 返回本次的基准用时区间（秒）。
     */
    fun baselineRange(baselineSecPerScore: ClosedRange<Double>, totalScore: Int): IntRange {
        val low = (baselineSecPerScore.start * totalScore).toInt()
        val high = (baselineSecPerScore.endInclusive * totalScore).toInt()
        return low..high
    }

    /**
     * @param timeSpentSec 本次实际用时
     * @param baseline     基准区间（用 baselineRange 算出）
     * @param accuracy     正确率 0..1
     * @param accuracyThreshold 达标线，默认 0.8
     * 用时达标判定：不超过基准上限即视为达标（鼓励，不苛求下限）。
     */
    fun evaluate(
        timeSpentSec: Int,
        baseline: IntRange,
        accuracy: Double,
        accuracyThreshold: Double = 0.8
    ): Quadrant {
        val onTime = timeSpentSec <= baseline.last
        val accurate = accuracy >= accuracyThreshold
        return when {
            onTime && accurate -> Quadrant.FAST_ACCURATE
            !onTime && accurate -> Quadrant.SLOW_ACCURATE
            onTime && !accurate -> Quadrant.FAST_INACCURATE
            else -> Quadrant.SLOW_INACCURATE
        }
    }
}
