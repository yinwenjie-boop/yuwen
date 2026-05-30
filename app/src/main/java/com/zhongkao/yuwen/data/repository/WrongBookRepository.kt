package com.zhongkao.yuwen.data.repository

import com.zhongkao.yuwen.data.db.WeakPointStat
import com.zhongkao.yuwen.data.db.WeakPointStatDao
import com.zhongkao.yuwen.data.db.WrongQuestion
import com.zhongkao.yuwen.data.db.WrongQuestionDao
import com.zhongkao.yuwen.data.db.WrongQuestionDetail
import kotlinx.coroutines.flow.Flow

/**
 * 错题本与薄弱点仓库（阶段 4）：未满分入错题本、按考点滚动薄弱点统计、供复习页归类与再练。
 */
class WrongBookRepository(
    private val wrongQuestionDao: WrongQuestionDao,
    private val weakPointStatDao: WeakPointStatDao
) {
    fun observeUnmastered(): Flow<List<WrongQuestion>> = wrongQuestionDao.observeUnmastered()
    fun observeUnmasteredCount(): Flow<Int> = wrongQuestionDao.observeUnmasteredCount()
    fun observeWrongDetails(): Flow<List<WrongQuestionDetail>> = wrongQuestionDao.observeDetails()

    fun observeTopWeakPoints(limit: Int = 3): Flow<List<WeakPointStat>> = weakPointStatDao.observeTop(limit)
    fun observeAllWeakPoints(): Flow<List<WeakPointStat>> = weakPointStatDao.observeAll()

    suspend fun addWrong(item: WrongQuestion): Long = wrongQuestionDao.insert(item)
    suspend fun markMastered(id: Long, mastered: Boolean = true) = wrongQuestionDao.setMastered(id, mastered)
    suspend fun upsertWeakPoint(stat: WeakPointStat) = weakPointStatDao.upsert(stat)
    suspend fun findWeakPoint(tag: String): WeakPointStat? = weakPointStatDao.find(tag)
}
