package com.zhongkao.yuwen.data.backup

import com.zhongkao.yuwen.data.db.AppDatabase
import com.zhongkao.yuwen.data.db.Attempt
import com.zhongkao.yuwen.data.db.Badge
import com.zhongkao.yuwen.data.db.Exercise
import com.zhongkao.yuwen.data.db.Question
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.data.db.WeakPointStat
import com.zhongkao.yuwen.data.db.WrongQuestion

/**
 * 备份/恢复所需的最小数据存取面。抽成接口便于 [BackupManager] 单测注入假实现。
 */
interface BackupStore {
    // —— 导出读取 ——
    suspend fun loadProgress(): UserProgress?
    suspend fun loadWeakPoints(): List<WeakPointStat>
    suspend fun loadBadges(): List<Badge>
    suspend fun loadWrongs(): List<WrongQuestion>
    suspend fun loadQuestion(id: Long): Question?
    suspend fun loadExercise(id: Long): Exercise?
    suspend fun loadLatestAttempt(questionId: Long): Attempt?

    // —— 导入写入 ——
    suspend fun saveProgress(p: UserProgress)
    suspend fun upsertWeakPoint(s: WeakPointStat)
    suspend fun insertBadge(b: Badge): Long
    suspend fun insertExercise(e: Exercise): Long
    suspend fun insertQuestion(q: Question): Long
    suspend fun insertAttempt(a: Attempt): Long
    suspend fun insertWrong(w: WrongQuestion): Long
}

/** Room 实现：直接走各 DAO。 */
class RoomBackupStore(private val db: AppDatabase) : BackupStore {
    override suspend fun loadProgress() = db.userProgressDao().get()
    override suspend fun loadWeakPoints() = db.weakPointStatDao().getAll()
    override suspend fun loadBadges() = db.badgeDao().getAll()
    override suspend fun loadWrongs() = db.wrongQuestionDao().getAll()
    override suspend fun loadQuestion(id: Long) = db.questionDao().findById(id)
    override suspend fun loadExercise(id: Long) = db.exerciseDao().findById(id)
    override suspend fun loadLatestAttempt(questionId: Long) = db.attemptDao().latestByQuestion(questionId)

    override suspend fun saveProgress(p: UserProgress) = db.userProgressDao().upsert(p)
    override suspend fun upsertWeakPoint(s: WeakPointStat) = db.weakPointStatDao().upsert(s)
    override suspend fun insertBadge(b: Badge): Long = db.badgeDao().insert(b)
    override suspend fun insertExercise(e: Exercise): Long = db.exerciseDao().insert(e)
    override suspend fun insertQuestion(q: Question): Long = db.questionDao().insertAll(listOf(q)).first()
    override suspend fun insertAttempt(a: Attempt): Long = db.attemptDao().insert(a)
    override suspend fun insertWrong(w: WrongQuestion): Long = db.wrongQuestionDao().insert(w)
}
