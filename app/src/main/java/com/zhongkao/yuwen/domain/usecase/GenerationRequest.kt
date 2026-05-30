package com.zhongkao.yuwen.domain.usecase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 出题大类（apiType 对齐 AiDtos.GeneratedExercise.type）。 */
@Serializable
enum class ExerciseType(val apiType: String, val label: String) {
    @SerialName("wenyan_compare") WENYAN_COMPARE("wenyan_compare", "文言对比阅读"),
    @SerialName("xiandai") XIANDAI("xiandai", "现代文阅读");

    companion object {
        fun fromApiType(t: String): ExerciseType? = entries.firstOrNull { it.apiType == t }
    }
}

/** 出题设置（写入 Exercise.configJson）。 */
@Serializable
data class GenerationRequest(
    val type: ExerciseType,
    val difficulty: String = "中等",       // 基础/中等/偏难
    val questionCount: Int = 4,
    val totalScore: Int = 15,
    val genre: String? = null,             // 现代文文体：记叙文/散文/说明文/议论文
    val focus: String? = null,             // 侧重考点 / 主题倾向
    val targetWords: Int = 800             // 现代文目标字数
)
