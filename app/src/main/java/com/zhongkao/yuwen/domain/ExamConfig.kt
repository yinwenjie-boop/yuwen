package com.zhongkao.yuwen.domain

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.seed.ExamConfigDto
import com.zhongkao.yuwen.data.seed.TimeBaselineDto

/**
 * 命题/时间基准配置（SPEC §7）。从 exam_config.json 解析而来，
 * 为 TimeEvaluator 提供"每分值基准秒数"区间与正确率达标阈值。
 *
 * type key 与 exam_config.json 的 time_baseline 子键一致：
 *  wenyan_compare / wenyan_single / xiandai_narrative / xiandai_expository
 */
class ExamConfig(
    private val baselines: Map<String, TimeBaseline>,
    val accuracyThreshold: Double,
    val meta: ExamConfigMeta
) {
    fun baseline(typeKey: String): TimeBaseline? = baselines[typeKey]

    /** 便捷：按题型与本次实际总分算出本次基准用时区间（秒）。 */
    fun baselineRange(typeKey: String, totalScore: Int): IntRange? =
        baseline(typeKey)?.let { TimeEvaluator.baselineRange(it.secPerScore, totalScore) }

    companion object {
        const val KEY_WENYAN_COMPARE = "wenyan_compare"
        const val KEY_WENYAN_SINGLE = "wenyan_single"
        const val KEY_XIANDAI_NARRATIVE = "xiandai_narrative"
        const val KEY_XIANDAI_EXPOSITORY = "xiandai_expository"

        fun parse(json: String): ExamConfig {
            val dto = AppJson.decodeFromString(ExamConfigDto.serializer(), json)
            val s = dto.timeBaseline
            val map = buildMap {
                s.wenyanCompare?.toModel()?.let { put(KEY_WENYAN_COMPARE, it) }
                s.wenyanSingle?.toModel()?.let { put(KEY_WENYAN_SINGLE, it) }
                s.xiandaiNarrative?.toModel()?.let { put(KEY_XIANDAI_NARRATIVE, it) }
                s.xiandaiExpository?.toModel()?.let { put(KEY_XIANDAI_EXPOSITORY, it) }
            }
            val meta = with(dto.meta) {
                ExamConfigMeta(
                    officialDocStatus = officialDocStatus,
                    examYear = examYear,
                    region = region,
                    calibrationInstruction = calibrationInstruction
                )
            }
            return ExamConfig(map, s.accuracyThreshold, meta)
        }

        private fun TimeBaselineDto.toModel(): TimeBaseline? {
            if (secPerScore.size < 2) return null
            val minutes = if (typicalMinutes.size >= 2) typicalMinutes[0]..typicalMinutes[1] else IntRange.EMPTY
            return TimeBaseline(
                secPerScore = secPerScore[0]..secPerScore[1],
                typicalMinutes = minutes,
                excellentMaxSec = excellentMaxSec
            )
        }
    }
}

data class TimeBaseline(
    val secPerScore: ClosedRange<Double>,  // 每分值基准秒数区间
    val typicalMinutes: IntRange,          // 单次练习人读区间(分钟)
    val excellentMaxSec: Int               // 优秀线(秒)
)

/**
 * 命题配置元信息（来自 exam_config.json 的 _meta），用于阶段 5 的校准入口展示。
 * [isCalibrated]=false 表示当前仍为「真题反推 + 公开资料」的估计值，尚未用官方命题说明校准。
 */
data class ExamConfigMeta(
    val officialDocStatus: String,
    val examYear: Int,
    val region: String,
    val calibrationInstruction: List<String>
) {
    /** PENDING_* 视为未校准；官方命题说明落地后状态应改为 official_* 等非 PENDING 值。 */
    val isCalibrated: Boolean
        get() = officialDocStatus.isNotBlank() &&
            !officialDocStatus.startsWith("PENDING", ignoreCase = true)
}
