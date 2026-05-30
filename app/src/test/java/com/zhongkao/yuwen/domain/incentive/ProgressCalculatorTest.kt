package com.zhongkao.yuwen.domain.incentive

import com.zhongkao.yuwen.data.db.UserProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressCalculatorTest {

    private val cfg = IncentiveConfig(
        xpPerScore = 10,
        targetBonusXp = 50,
        coinsPerScore = 2,
        streakDailyCoins = 5,
        xpPerLevel = 300
    )
    private val today = LocalDate.of(2026, 5, 30)

    @Test
    fun `首次练习达标_XP含奖励且连胜从1开始`() {
        val out = ProgressCalculator.apply(
            current = UserProgress(),
            today = today,
            totalGot = 12,
            targetMet = true,
            config = cfg
        )
        // 12*10 + 50 = 170
        assertEquals(170, out.xpGained)
        // 12*2 + 5(当日首练) = 29
        assertEquals(29, out.coinsGained)
        assertEquals(1, out.newStreak)
        assertTrue(out.streakIncreased)
        assertEquals(today.toString(), out.progress.lastPracticeDate)
    }

    @Test
    fun `未达标不发达标奖励`() {
        val out = ProgressCalculator.apply(UserProgress(), today, totalGot = 10, targetMet = false, config = cfg)
        assertEquals(100, out.xpGained)  // 仅 10*10
    }

    @Test
    fun `昨天练过_今天连胜加一`() {
        val current = UserProgress(streakDays = 3, lastPracticeDate = today.minusDays(1).toString())
        val out = ProgressCalculator.apply(current, today, totalGot = 5, targetMet = false, config = cfg)
        assertEquals(4, out.newStreak)
        assertTrue(out.streakIncreased)
        assertEquals(15, out.coinsGained)  // 5*2 + 5 当日打卡 = 15
    }

    @Test
    fun `当日重复练习_不重复计连胜与打卡积分`() {
        val current = UserProgress(streakDays = 2, lastPracticeDate = today.toString())
        val out = ProgressCalculator.apply(current, today, totalGot = 5, targetMet = false, config = cfg)
        assertEquals(2, out.newStreak)
        assertFalse(out.streakIncreased)
        assertEquals(10, out.coinsGained)  // 5*2，无打卡加成
    }

    @Test
    fun `断签后连胜归一`() {
        val current = UserProgress(streakDays = 9, lastPracticeDate = today.minusDays(3).toString())
        val out = ProgressCalculator.apply(current, today, totalGot = 5, targetMet = false, config = cfg)
        assertEquals(1, out.newStreak)
        assertTrue(out.streakIncreased)
    }

    @Test
    fun `累计XP跨阈值时升级且换称号`() {
        val current = UserProgress(xp = 290, level = 1, levelTitle = "文言学徒")
        val out = ProgressCalculator.apply(current, today, totalGot = 5, targetMet = false, config = cfg)
        // 290 + 50 = 340 → level 2
        assertEquals(340, out.progress.xp)
        assertEquals(2, out.progress.level)
        assertTrue(out.leveledUp)
        assertEquals(LevelSystem.titleForLevel(2), out.progress.levelTitle)
    }
}
