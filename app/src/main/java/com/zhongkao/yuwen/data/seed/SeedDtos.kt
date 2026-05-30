package com.zhongkao.yuwen.data.seed

import com.zhongkao.yuwen.data.ai.Note
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 种子语料文件结构（对应 assets/seed/kewen_seed.json、kewai_seed.json）。
 * 字段与 JSON 严格对齐；多余的 _meta 等键由 AppJson(ignoreUnknownKeys) 忽略。
 */
@Serializable
data class SeedTextFile(
    val items: List<SeedTextItem> = emptyList()
)

@Serializable
data class SeedTextItem(
    @SerialName("id") val id: String,
    @SerialName("category") val category: String,                 // 课内 / 课外
    @SerialName("title") val title: String,
    @SerialName("author") val author: String = "",
    @SerialName("dynasty") val dynasty: String = "",
    @SerialName("grade") val grade: String = "",
    @SerialName("theme") val theme: String = "",
    @SerialName("ability_focus") val abilityFocus: List<String> = emptyList(),
    @SerialName("link_hint") val linkHint: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("notes") val notes: List<Note> = emptyList(),
    @SerialName("translation") val translation: String = "",
    @SerialName("source_ref") val sourceRef: String = "",
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("need_human_review") val needHumanReview: Boolean = false
)
