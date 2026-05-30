package com.zhongkao.yuwen.domain.incentive

import com.zhongkao.yuwen.data.ai.BadgeAward
import com.zhongkao.yuwen.domain.TimeEvaluator

/**
 * 徽章规则（SPEC §8）：按 §7.3 四象限盖一枚象限章，外加连胜 / 升级里程碑章。纯函数。
 */
object BadgeRules {

    private val QUADRANT_NAME = mapOf(
        TimeEvaluator.Quadrant.FAST_ACCURATE.code to "神速准星",
        TimeEvaluator.Quadrant.SLOW_ACCURATE.code to "稳扎稳打",
        TimeEvaluator.Quadrant.FAST_INACCURATE.code to "急中求稳",
        TimeEvaluator.Quadrant.SLOW_INACCURATE.code to "厚积待发"
    )

    /** 连胜里程碑天数 → 徽章名。 */
    val STREAK_MILESTONES = mapOf(3 to "三日连胜", 7 to "连胜达人", 14 to "半月不辍", 30 to "月度学霸")

    /** 本次象限章（用于展示与发章）；无基准时返回 null。 */
    fun quadrantBadge(quadrantCode: String?): BadgeAward? =
        quadrantCode?.let { code -> QUADRANT_NAME[code]?.let { BadgeAward(code, it) } }

    /** 连胜 / 升级里程碑章（可能为空）。 */
    fun milestones(outcome: ProgressCalculator.Outcome): List<BadgeAward> = buildList {
        if (outcome.streakIncreased) {
            STREAK_MILESTONES[outcome.newStreak]?.let {
                add(BadgeAward("streak_${outcome.newStreak}", it))
            }
        }
        if (outcome.leveledUp) {
            add(BadgeAward("level_${outcome.progress.level}", "等级跃升 · Lv${outcome.progress.level} ${outcome.progress.levelTitle}"))
        }
    }
}
