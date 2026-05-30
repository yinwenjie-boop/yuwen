package com.zhongkao.yuwen.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * exam_config.json 的解析结构（只取本阶段需要的 time_baseline）。
 * 其余 _meta/paper/sections/kewen_scope 等键由 ignoreUnknownKeys 忽略。
 */
@Serializable
data class ExamConfigDto(
    @SerialName("time_baseline") val timeBaseline: TimeBaselineSectionDto = TimeBaselineSectionDto()
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
