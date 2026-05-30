package com.zhongkao.yuwen.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * exam_config.json 的解析结构（只取本阶段需要的 time_baseline）。
 * 其余 _meta/paper/sections/kewen_scope 等键由 ignoreUnknownKeys 忽略。
 */
@Serializable
data class ExamConfigDto(
    @SerialName("_meta") val meta: ExamConfigMetaDto = ExamConfigMetaDto(),
    @SerialName("time_baseline") val timeBaseline: TimeBaselineSectionDto = TimeBaselineSectionDto()
)

/** exam_config.json 的 _meta：校准状态与校准指引（阶段 5 校准入口展示用）。 */
@Serializable
data class ExamConfigMetaDto(
    @SerialName("official_doc_status") val officialDocStatus: String = "",
    @SerialName("exam_year") val examYear: Int = 0,
    @SerialName("region") val region: String = "",
    @SerialName("calibration_instruction") val calibrationInstruction: List<String> = emptyList()
)

@Serializable
data class TimeBaselineSectionDto(
    @SerialName("wenyan_compare") val wenyanCompare: TimeBaselineDto? = null,
    @SerialName("wenyan_single") val wenyanSingle: TimeBaselineDto? = null,
    @SerialName("xiandai_narrative") val xiandaiNarrative: TimeBaselineDto? = null,
    @SerialName("xiandai_expository") val xiandaiExpository: TimeBaselineDto? = null,
    @SerialName("accuracy_threshold") val accuracyThreshold: Double = 0.8
)

@Serializable
data class TimeBaselineDto(
    @SerialName("typical_minutes") val typicalMinutes: List<Int> = emptyList(),
    @SerialName("sec_per_score") val secPerScore: List<Double> = emptyList(),
    @SerialName("excellent_max_sec") val excellentMaxSec: Int = 0
)
