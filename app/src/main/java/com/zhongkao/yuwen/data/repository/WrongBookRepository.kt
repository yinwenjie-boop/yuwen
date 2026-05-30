package com.zhongkao.yuwen.data.repository

import com.zhongkao.yuwen.data.db.WeakPointStat
import com.zhongkao.yuwen.data.db.WeakPointStatDao
import com.zhongkao.yuwen.data.db.WrongQuestion
import com.zhongkao.yuwen.data.db.WrongQuestionDao
import kotlinx.coroutines.flow.Flow

/**
 * 错题本与薄弱点仓库（骨架）。用时曲线/按薄弱点再练在阶段 4 实现。
 */
class WrongBookRepository(
    private val wrongQuestionDao: WrongQuestionDao,
    private val weakPointStatDao: WeakPointStatDao
) {
    fun observeUnmastered(): Flow<List<WrongQuestion>> = wrongQuestionDao.observeUnmastered()
    fun observeUnmasteredCount(): Flow<Int> = wrongQuestionDao.observeUnmasteredCount()
    fun observeTopWeakPoints(limit: Int = 3): Flow<List<WeakPointStat>> = weakPointStatDao.observeTop(limit)

    suspend fun addWrong(item: WrongQuestion): Long = wrongQuestionDao.insert(item)
    suspend fun markMastered(id: Long, mastered: Boolean = true) = wrongQuestionDao.setMastered(id, mastered)
    suspend fun upsertWeakPoint(stat: WeakPointStat) = weakPointStatDao.upsert(stat)
    suspend fun findWeakPoint(tag: String): WeakPointStat? = weakPointStatDao.find(tag)
}
