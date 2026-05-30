package com.zhongkao.yuwen.domain.incentive

import com.zhongkao.yuwen.data.db.WeakPointStat
import org.junit.Assert.assertEquals
import org.junit.Test

class WeakPointAccumulatorTest {

    @Test
    fun `首次统计直接落本轮值`() {
        val s = WeakPointAccumulator.merge(
            existing = null, tag = "文言实词",
            roundAttempts = 3, roundWrong = 2, roundTimeSumSec = 180, now = 100L
        )
        assertEquals("文言实词", s.abilityTag)
        assertEquals(3, s.attempts)
        assertEquals(2, s.wrongCount)
        assertEquals(60, s.avgTimeSec)  // 180/3
        assertEquals(100L, s.lastUpdated)
    }

    @Test
    fun `并入历史时按总次数加权平均用时`() {
        val existing = WeakPointStat(abilityTag = "句子翻译", attempts = 2, wrongCount = 1, avgTimeSec = 50)
        val s = WeakPointAccumulator.merge(
            existing = existing, tag = "句子翻译",
            roundAttempts = 2, roundWrong = 2, roundTimeSumSec = 150, now = 200L
        )
        assertEquals(4, s.attempts)
        assertEquals(3, s.wrongCount)
        // (50*2 + 150) / 4 = 62
        assertEquals(62, s.avgTimeSec)
    }
}
