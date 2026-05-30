package com.zhongkao.yuwen.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeEvaluatorTest {

    @Test
    fun `按每分值秒数与总分归一化出基准区间`() {
        // wenyan_compare: sec_per_score = [64, 80]，总分 15 → [960, 1200]
        val range = TimeEvaluator.baselineRange(64.0..80.0, 15)
        assertEquals(960, range.first)
        assertEquals(1200, range.last)
    }

    @Test
    fun `用时达标且正确率达标为又快又准`() {
        val q = TimeEvaluator.evaluate(timeSpentSec = 1000, baseline = 960..1200, accuracy = 0.9)
        assertEquals(TimeEvaluator.Quadrant.FAST_ACCURATE, q)
    }

    @Test
    fun `超时但正确率高为该提速`() {
        val q = TimeEvaluator.evaluate(timeSpentSec = 1500, baseline = 960..1200, accuracy = 0.9)
        assertEquals(TimeEvaluator.Quadrant.SLOW_ACCURATE, q)
    }

    @Test
    fun `用时达标但正确率低为求快丢分`() {
        val q = TimeEvaluator.evaluate(timeSpentSec = 900, baseline = 960..1200, accuracy = 0.5)
        assertEquals(TimeEvaluator.Quadrant.FAST_INACCURATE, q)
    }

    @Test
    fun `超时且正确率低为基础待巩固`() {
        val q = TimeEvaluator.evaluate(timeSpentSec = 1500, baseline = 960..1200, accuracy = 0.5)
        assertEquals(TimeEvaluator.Quadrant.SLOW_INACCURATE, q)
    }

    @Test
    fun `恰好等于基准上限视为达标`() {
        val q = TimeEvaluator.evaluate(timeSpentSec = 1200, baseline = 960..1200, accuracy = 0.8)
        assertEquals(TimeEvaluator.Quadrant.FAST_ACCURATE, q)
    }
}
