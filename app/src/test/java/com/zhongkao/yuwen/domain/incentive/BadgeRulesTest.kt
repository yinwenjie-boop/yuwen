package com.zhongkao.yuwen.domain.incentive

import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.domain.TimeEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeRulesTest {

    @Test
    fun `四象限映射到象限章`() {
        val b = BadgeRules.quadrantBadge(TimeEvaluator.Quadrant.FAST_ACCURATE.code)
        assertEquals(TimeEvaluator.Quadrant.FAST_ACCURATE.code, b?.code)
        assertEquals("神速准星", b?.name)
    }

    @Test
    fun `无象限码不发章`() {
        assertNull(BadgeRules.quadrantBadge(null))
        assertNull(BadgeRules.quadrantBadge("unknown"))
    }

    @Test
    fun `连胜达到里程碑且为当日首练才发连胜章`() {
        val out = ProgressCalculator.Outcome(
            xpGained = 0, coinsGained = 0, newStreak = 7,
            streakIncreased = true, leveledUp = false,
            progress = UserProgress(streakDays = 7)
        )
        val names = BadgeRules.milestones(out).map { it.name }
        assertTrue(names.contains("连胜达人"))
    }

    @Test
    fun `非当日首练不发连胜章`() {
        val out = ProgressCalculator.Outcome(
            xpGained = 0, coinsGained = 0, newStreak = 7,
            streakIncreased = false, leveledUp = false,
            progress = UserProgress(streakDays = 7)
        )
        assertTrue(BadgeRules.milestones(out).isEmpty())
    }

    @Test
    fun `升级时发升级章`() {
        val out = ProgressCalculator.Outcome(
            xpGained = 0, coinsGained = 0, newStreak = 2,
            streakIncreased = true, leveledUp = true,
            progress = UserProgress(level = 3, levelTitle = "文言高手")
        )
        val codes = BadgeRules.milestones(out).map { it.code }
        assertTrue(codes.contains("level_3"))
    }
}
