package com.zhongkao.yuwen.domain.incentive

/**
 * 等级 / 称号体系（SPEC §8）。XP 线性升级，称号随等级递进。
 * 纯函数，便于单测；称号超出列表时停在最高称号。
 */
object LevelSystem {

    /** 称号序列（文言学徒 → … → 文言大儒）。 */
    val TITLES = listOf("文言学徒", "文言行家", "文言高手", "文言宗师", "文言大儒")

    /** 由累计 XP 算等级（1 起步）。 */
    fun levelForXp(xp: Int, xpPerLevel: Int): Int {
        if (xpPerLevel <= 0) return 1
        return (xp.coerceAtLeast(0) / xpPerLevel) + 1
    }

    /** 由等级取称号。 */
    fun titleForLevel(level: Int): String {
        val idx = (level - 1).coerceIn(0, TITLES.lastIndex)
        return TITLES[idx]
    }

    /** 当前等级内已累计的 XP（用于进度条）。 */
    fun xpIntoLevel(xp: Int, xpPerLevel: Int): Int {
        if (xpPerLevel <= 0) return 0
        return xp.coerceAtLeast(0) % xpPerLevel
    }
}
