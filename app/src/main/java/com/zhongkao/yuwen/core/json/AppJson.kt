package com.zhongkao.yuwen.core.json

import kotlinx.serialization.json.Json

/**
 * 全局 Json 实例（对应 AiDtos.kt 注释要求）。
 *  - ignoreUnknownKeys：模型可能多返字段；
 *  - coerceInputValues：null/缺省值回落到默认值，配合 DTO 的默认值更稳。
 * 配合 DeepSeekApi.extractJson() 先清洗再解析。
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}
