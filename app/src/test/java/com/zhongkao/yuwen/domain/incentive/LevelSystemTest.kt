package com.zhongkao.yuwen.domain.incentive

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelSystemTest {

    @Test
    fun `等级按xpPerLevel线性递增_从1起步`() {
        assertEquals(1, LevelSystem.levelForXp(0, 300))
        assertEquals(1, LevelSystem.levelForXp(299, 300))
        assertEquals(2, LevelSystem.levelForXp(300, 300))
        assertEquals(4, LevelSystem.levelForXp(900, 300))
    }

    @Test
    fun `称号随等级递进且封顶在最高称号`() {
        assertEquals("文言学徒", LevelSystem.titleForLevel(1))
        assertEquals("文言行家", LevelSystem.titleForLevel(2))
        assertEquals(LevelSystem.TITLES.last(), LevelSystem.titleForLevel(99))
    }

    @Test
    fun `当前等级内进度取模`() {
        assertEquals(50, LevelSystem.xpIntoLevel(350, 300))
        assertEquals(0, LevelSystem.xpIntoLevel(300, 300))
    }

    @Test
    fun `异常xpPerLevel不崩`() {
        assertEquals(1, LevelSystem.levelForXp(500, 0))
        assertEquals(0, LevelSystem.xpIntoLevel(500, 0))
    }
}
