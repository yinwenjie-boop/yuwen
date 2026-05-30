package com.zhongkao.yuwen.domain.incentive

import com.zhongkao.yuwen.data.db.UserProgress
import java.time.LocalDate

/**
 * 进度结算（SPEC §8）：由本次得分 + 是否达标 + 连续打卡，纯函数算出
 * XP / 积分增量与新的 [UserProgress]。所有数值本地计算，"今天"由调用方注入便于单测。
 */
object ProgressCalculator {

    data class Outcome(
        val xpGained: Int,
        val coinsGained: Int,
        val newStreak: Int,
        val streakIncreased: Boolean,  // 是否为当日首练（连续天数推进，可触发里程碑/打卡积分）
        val leveledUp: Boolean,
        val progress: UserProgress
    )

    /**
     * @param current   当前进度（单行）
     * @param today     今天（ISO 本地日期）
     * @param totalGot  本次总得分
     * @param targetMet 本次是否达标（又快又准）
     */
    fun apply(
        current: UserProgress,
        today: LocalDate,
        totalGot: Int,
        targetMet: Boolean,
        config: IncentiveConfig
    ): Outcome {
        val last = current.lastPracticeDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val (newStreak, isNewDay) = when {
            last == null -> 1 to true                                   // 首次练习
            last.isEqual(today) -> current.streakDays.coerceAtLeast(1) to false  // 当日重复，不重复计打卡
            last.isEqual(today.minusDays(1)) -> current.streakDays + 1 to true   // 昨天练过，连续 +1
            last.isBefore(today) -> 1 to true                          // 断签，重新计 1
            else -> current.streakDays.coerceAtLeast(1) to false       // 异常(未来日期)，不变
        }

        val got = totalGot.coerceAtLeast(0)
        val xpGained = got * config.xpPerScore + (if (targetMet) config.targetBonusXp else 0)
        val coinsGained = got * config.coinsPerScore + (if (isNewDay) config.streakDailyCoins else 0)

        val newXp = current.xp + xpGained
        val newLevel = LevelSystem.levelForXp(newXp, config.xpPerLevel)
        val newTitle = LevelSystem.titleForLevel(newLevel)
        val leveledUp = newLevel > current.level

        val progress = current.copy(
            xp = newXp,
            level = newLevel,
            levelTitle = newTitle,
            coins = current.coins + coinsGained,
            streakDays = newStreak,
            lastPracticeDate = today.toString()
        )
        return Outcome(
            xpGained = xpGained,
            coinsGained = coinsGained,
            newStreak = newStreak,
            streakIncreased = isNewDay,
            leveledUp = leveledUp,
            progress = progress
        )
    }
}
