package com.zhongkao.yuwen.data.repository

import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.data.db.TextBankDao
import kotlinx.coroutines.flow.Flow

/**
 * 文言语料库仓库（骨架）。
 * 防伪职责（后续阶段实现）：课内固定语料只读；课外篇优先选库；
 * AI 提议课外篇 verified=false 时不得经此入正式题库（由 AiResponseValidator 拦截）。
 */
class TextBankRepository(private val dao: TextBankDao) {

    fun observeKewen(): Flow<List<TextBank>> = dao.observeByCategory(CATEGORY_KEWEN)
    fun observeKewai(): Flow<List<TextBank>> = dao.observeByCategory(CATEGORY_KEWAI)

    fun observeKewenCount(): Flow<Int> = dao.observeCountByCategory(CATEGORY_KEWEN)
    fun observeKewaiCount(): Flow<Int> = dao.observeCountByCategory(CATEGORY_KEWAI)

    suspend fun findById(id: Long): TextBank? = dao.findById(id)
    suspend fun findByTitle(title: String): TextBank? = dao.findByTitle(title)
    suspend fun count(): Int = dao.count()

    /** 仅供语料导入流程使用（阶段 1）。 */
    suspend fun seed(items: List<TextBank>): List<Long> = dao.insertAll(items)

    companion object {
        const val CATEGORY_KEWEN = "课内"
        const val CATEGORY_KEWAI = "课外"
    }
}
