package com.zhongkao.yuwen.data.repository

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.ai.Note
import com.zhongkao.yuwen.data.ai.Passage
import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.data.db.TextBankDao
import com.zhongkao.yuwen.domain.usecase.PassageSource
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer

/**
 * 文言语料库仓库。
 * 防伪职责：课内固定语料只读；课外篇优先选库（仅 verified=true 可被出题选中）；
 * AI 提议课外篇一律 verified=false 入「待确认」，须经人工确认（[confirmPending]）方可入正式题库。
 */
class TextBankRepository(private val dao: TextBankDao) : PassageSource {

    fun observeKewen(): Flow<List<TextBank>> = dao.observeByCategory(CATEGORY_KEWEN)
    fun observeKewai(): Flow<List<TextBank>> = dao.observeByCategory(CATEGORY_KEWAI)

    fun observeKewenCount(): Flow<Int> = dao.observeCountByCategory(CATEGORY_KEWEN)
    fun observeKewaiCount(): Flow<Int> = dao.observeCountByCategory(CATEGORY_KEWAI)

    suspend fun findById(id: Long): TextBank? = dao.findById(id)
    suspend fun findByTitle(title: String): TextBank? = dao.findByTitle(title)

    /** 随机取一篇已确认的课内/课外篇用于出题选篇。 */
    override suspend fun randomKewen(): TextBank? = dao.randomVerified(CATEGORY_KEWEN)
    override suspend fun randomKewai(): TextBank? = dao.randomVerified(CATEGORY_KEWAI)
    suspend fun count(): Int = dao.count()

    /** 仅供语料导入流程使用（阶段 1）。 */
    suspend fun seed(items: List<TextBank>): List<Long> = dao.insertAll(items)

    // —— 阶段 5：AI 提议课外篇的人工确认流 ——

    /** 待人工确认的课外篇（verified=false），按新到旧。 */
    fun observePendingReview(): Flow<List<TextBank>> = dao.observePending(CATEGORY_KEWAI)

    /** 待确认篇数（供首页入口角标）。 */
    fun observePendingCount(): Flow<Int> = dao.observePendingCount(CATEGORY_KEWAI)

    /** 人工确认真实性 → verified=true，自此可被出题选中。 */
    suspend fun confirmPending(id: Long) = dao.setVerified(id, true)

    /** 拒绝（出处存疑/不合规）→ 直接删除，绝不入库。 */
    suspend fun rejectPending(id: Long) = dao.deleteById(id)

    /**
     * AI 提议的课外篇存入「待确认」（verified=false）。
     * seedId 用 title+source_ref 派生，保证同一提议重复出题时幂等覆盖、不堆重复条目。
     */
    override suspend fun savePendingKewai(passage: Passage): Long {
        val notesJson = AppJson.encodeToString(ListSerializer(Note.serializer()), passage.notes)
        return dao.insert(
            TextBank(
                seedId = "ai_" + Integer.toHexString((passage.title + "|" + passage.sourceRef).hashCode()),
                category = CATEGORY_KEWAI,
                title = passage.title,
                author = passage.author,
                dynasty = passage.dynasty,
                text = passage.text,
                notes = notesJson,
                sourceRef = passage.sourceRef,
                verified = false
            )
        )
    }

    companion object {
        const val CATEGORY_KEWEN = "课内"
        const val CATEGORY_KEWAI = "课外"
    }
}
