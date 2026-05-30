package com.zhongkao.yuwen.data.repository

import com.zhongkao.yuwen.data.db.Badge
import com.zhongkao.yuwen.data.db.BadgeDao
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.data.db.UserProgressDao
import kotlinx.coroutines.flow.Flow

/**
 * 进度与激励仓库（骨架）：XP/等级/积分/连续打卡 + 徽章。
 * 所有激励数值在端上计算（SPEC §8），此处只负责读写。
 */
class ProgressRepository(
    private val userProgressDao: UserProgressDao,
    private val badgeDao: BadgeDao
) {
    fun observeProgress(): Flow<UserProgress?> = userProgressDao.observe()
    suspend fun getProgress(): UserProgress? = userProgressDao.get()
    suspend fun saveProgress(progress: UserProgress) = userProgressDao.upsert(progress)

    fun observeBadges(): Flow<List<Badge>> = badgeDao.observeAll()
    suspend fun awardBadge(badge: Badge): Long = badgeDao.insert(badge)

    /** 首次启动确保单行进度存在。 */
    suspend fun ensureInitialized() {
        if (userProgressDao.get() == null) {
            userProgressDao.upsert(UserProgress())
        }
    }
}
