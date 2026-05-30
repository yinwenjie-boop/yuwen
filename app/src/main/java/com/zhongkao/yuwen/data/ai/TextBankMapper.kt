package com.zhongkao.yuwen.data.ai

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.db.TextBank
import kotlinx.serialization.builtins.ListSerializer

/**
 * 本地语料 → AI 契约 Passage 的映射。
 * 防伪关键：出题后用本地真实原文覆盖 AI 返回的 passage，确保课内/课外原文不被模型改动。
 */
fun TextBank.toPassage(): Passage {
    val parsedNotes = runCatching {
        AppJson.decodeFromString(ListSerializer(Note.serializer()), notes)
    }.getOrDefault(emptyList())
    return Passage(
        title = title,
        author = author,
        dynasty = dynasty,
        text = text,
        notes = parsedNotes,
        sourceRef = sourceRef,
        verified = verified,
        needHumanReview = false
    )
}
