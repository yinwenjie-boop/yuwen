package com.zhongkao.yuwen.data.seed

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.data.repository.TextBankRepository
import com.zhongkao.yuwen.data.ai.Note
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * 种子语料导入（纯逻辑，可单测；不依赖 Android）。
 *
 * 防伪硬约束（SPEC §2）落地点：
 *  - 课内篇：必须 verified=true 且原文非空，才视为"固定语料"入库；
 *  - 课外篇：必须 verified=true、不处于待人工确认(need_human_review=false)、且有出处(source_ref)；
 *  - 不满足者一律跳过，并记录原因，绝不入库（更不会用 AI 生成课内原文）。
 */
object SeedImporter {

    data class ImportResult(
        val importable: List<TextBank>,
        val skipped: List<Skipped>
    ) {
        val kewenCount: Int get() = importable.count { it.category == TextBankRepository.CATEGORY_KEWEN }
        val kewaiCount: Int get() = importable.count { it.category == TextBankRepository.CATEGORY_KEWAI }
    }

    data class Skipped(val id: String, val title: String, val reason: String)

    fun parse(json: String): List<SeedTextItem> =
        AppJson.decodeFromString(SeedTextFile.serializer(), json).items

    /** 解析 + 防伪过滤 + 映射为实体。 */
    fun import(json: String): ImportResult {
        val items = parse(json)
        val importable = mutableListOf<TextBank>()
        val skipped = mutableListOf<Skipped>()
        for (item in items) {
            val reason = rejectionReason(item)
            if (reason == null) importable += toEntity(item)
            else skipped += Skipped(item.id, item.title, reason)
        }
        return ImportResult(importable, skipped)
    }

    /** 返回拒绝原因；null 表示可入库。 */
    fun rejectionReason(item: SeedTextItem): String? {
        if (item.text.isBlank()) return "原文为空"
        if (!item.verified) return "verified=false（未确认真实性）"
        return when (item.category) {
            TextBankRepository.CATEGORY_KEWEN -> null
            TextBankRepository.CATEGORY_KEWAI -> when {
                item.needHumanReview -> "课外篇待人工确认(need_human_review=true)"
                item.sourceRef.isBlank() -> "课外篇缺出处(source_ref)"
                else -> null
            }
            else -> "未知 category: ${item.category}"
        }
    }

    fun toEntity(item: SeedTextItem): TextBank = TextBank(
        seedId = item.id,
        category = item.category,
        title = item.title,
        author = item.author,
        dynasty = item.dynasty,
        grade = item.grade,
        text = item.text,
        notes = AppJson.encodeToString(ListSerializer(Note.serializer()), item.notes),
        translation = item.translation,
        abilityFocus = AppJson.encodeToString(ListSerializer(String.serializer()), item.abilityFocus),
        sourceRef = item.sourceRef,
        theme = item.theme,
        linkHint = item.linkHint,
        verified = item.verified
    )
}
