package com.zhongkao.yuwen.domain.incentive

/**
 * 激励数值配置（SPEC §8：数值本地计算、设置页可调）。
 * 全部为整数系数，便于在设置页用数字输入框调节。
 */
data class IncentiveConfig(
    val xpPerScore: Int = DEFAULT_XP_PER_SCORE,           // 每得 1 分 → XP
    val targetBonusXp: Int = DEFAULT_TARGET_BONUS_XP,     // 达标练习(又快又准)额外 XP
    val coinsPerScore: Int = DEFAULT_COINS_PER_SCORE,     // 每得 1 分 → 积分
    val streakDailyCoins: Int = DEFAULT_STREAK_DAILY_COINS, // 当日首练打卡积分
    val xpPerLevel: Int = DEFAULT_XP_PER_LEVEL            // 每级所需 XP（线性）
) {
    companion object {
        const val DEFAULT_XP_PER_SCORE = 10
        const val DEFAULT_TARGET_BONUS_XP = 50
        const val DEFAULT_COINS_PER_SCORE = 2
        const val DEFAULT_STREAK_DAILY_COINS = 5
        const val DEFAULT_XP_PER_LEVEL = 300
    }
}
