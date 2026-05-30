package com.zhongkao.yuwen.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTimerTest {

    /** 可控假时钟（毫秒）。 */
    private class FakeClock(var now: Long = 0L) {
        fun tick(ms: Long) { now += ms }
        val source: () -> Long = { now }
    }

    @Test
    fun `单题计时累计到秒`() {
        val clock = FakeClock()
        val timer = ExerciseTimer(clock.source)
        timer.start("q1")
        clock.tick(3000)
        val snap = timer.stop()
        assertEquals(3, snap.totalSec)
        assertEquals(3, snap.perQuestion.single().sec)
    }

    @Test
    fun `切换题目分别累计且能定位最耗时题`() {
        val clock = FakeClock()
        val timer = ExerciseTimer(clock.source)
        timer.start("q1")
        clock.tick(2000)        // q1: 2s
        timer.switchTo("q2")
        clock.tick(5000)        // q2: 5s
        timer.switchTo("q3")
        clock.tick(1000)        // q3: 1s
        val snap = timer.stop()

        assertEquals(8, snap.totalSec)
        assertEquals(2, snap.perQuestion.first { it.id == "q1" }.sec)
        assertEquals(5, snap.perQuestion.first { it.id == "q2" }.sec)
        assertEquals(1, snap.perQuestion.first { it.id == "q3" }.sec)
        assertEquals("q2", snap.slowest?.id)
    }

    @Test
    fun `后台暂停期间不计时`() {
        val clock = FakeClock()
        val timer = ExerciseTimer(clock.source)
        timer.start("q1")
        clock.tick(2000)        // 计入 2s
        timer.pause()
        clock.tick(10000)       // 后台 10s 不计
        timer.resume()
        clock.tick(1000)        // 再计 1s
        val snap = timer.stop()
        assertEquals(3, snap.totalSec)
    }

    @Test
    fun `回到已答过的题继续累加`() {
        val clock = FakeClock()
        val timer = ExerciseTimer(clock.source)
        timer.start("q1")
        clock.tick(2000)
        timer.switchTo("q2")
        clock.tick(3000)
        timer.switchTo("q1")    // 回到 q1
        clock.tick(4000)
        val snap = timer.stop()
        assertEquals(6, snap.perQuestion.first { it.id == "q1" }.sec) // 2 + 4
        assertEquals(3, snap.perQuestion.first { it.id == "q2" }.sec)
        assertEquals(9, snap.totalSec)
    }
}
