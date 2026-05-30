package com.zhongkao.yuwen.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.zhongkao.yuwen.domain.incentive.IncentiveConfig

/**
 * 激励数值的本地持久化（SPEC §8：设置页可调、本地存）。
 * 非敏感数据，用普通 SharedPreferences；未设置时回落 [IncentiveConfig] 默认值。
 */
class IncentiveSettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun getConfig(): IncentiveConfig {
        val d = IncentiveConfig()
        return IncentiveConfig(
            xpPerScore = prefs.getInt(KEY_XP_PER_SCORE, d.xpPerScore),
            targetBonusXp = prefs.getInt(KEY_TARGET_BONUS_XP, d.targetBonusXp),
            coinsPerScore = prefs.getInt(KEY_COINS_PER_SCORE, d.coinsPerScore),
            streakDailyCoins = prefs.getInt(KEY_STREAK_DAILY_COINS, d.streakDailyCoins),
            xpPerLevel = prefs.getInt(KEY_XP_PER_LEVEL, d.xpPerLevel)
        )
    }

    fun setConfig(config: IncentiveConfig) {
        prefs.edit()
            .putInt(KEY_XP_PER_SCORE, config.xpPerScore.coerceAtLeast(0))
            .putInt(KEY_TARGET_BONUS_XP, config.targetBonusXp.coerceAtLeast(0))
            .putInt(KEY_COINS_PER_SCORE, config.coinsPerScore.coerceAtLeast(0))
            .putInt(KEY_STREAK_DAILY_COINS, config.streakDailyCoins.coerceAtLeast(0))
            .putInt(KEY_XP_PER_LEVEL, config.xpPerLevel.coerceAtLeast(1))
            .apply()
    }

    private companion object {
        const val PREF_FILE = "incentive_settings"
        const val KEY_XP_PER_SCORE = "xp_per_score"
        const val KEY_TARGET_BONUS_XP = "target_bonus_xp"
        const val KEY_COINS_PER_SCORE = "coins_per_score"
        const val KEY_STREAK_DAILY_COINS = "streak_daily_coins"
        const val KEY_XP_PER_LEVEL = "xp_per_level"
    }
}
