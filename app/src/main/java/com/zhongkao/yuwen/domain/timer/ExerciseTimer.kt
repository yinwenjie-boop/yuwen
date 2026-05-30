package com.zhongkao.yuwen.domain.timer

/**
 * 答题计时器（SPEC §7.2 计时规则）：
 *  - 进入答题页 start()；
 *  - 切换题目 switchTo() 记录上一题用时；
 *  - 切到后台 pause()，回前台 resume()（后台不计时）；
 *  - 提交批改 stop() 停表并产出本次用时快照。
 *
 * 设计为纯逻辑 + 可注入时钟，便于单测。时钟必须单调递增（毫秒）；
 * Android 侧传 SystemClock.elapsedRealtime；测试传可控的假时钟。
 */
class ExerciseTimer(
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private val accumulatedMs = LinkedHashMap<String, Long>() // questionId -> 累计毫秒
    private var currentId: String? = null
    private var segmentStartMs: Long = 0
    private var running = false
    private var paused = false

    val isRunning: Boolean get() = running && !paused

    /** 进入答题页，从第一题开始计时。 */
    fun start(firstQuestionId: String) {
        accumulatedMs.clear()
        accumulatedMs[firstQuestionId] = 0
        currentId = firstQuestionId
        segmentStartMs = clockMs()
        running = true
        paused = false
    }

    /** 切换到另一题：先把当前段计入上一题，再开始新题计时。 */
    fun switchTo(questionId: String) {
        if (!running) return
        commitSegment()
        currentId = questionId
        accumulatedMs.getOrPut(questionId) { 0 }
        if (!paused) segmentStartMs = clockMs()
    }

    /** 切后台：把当前段累计后暂停。 */
    fun pause() {
        if (!running || paused) return
        commitSegment()
        paused = true
    }

    /** 回前台：恢复计时。 */
    fun resume() {
        if (!running || !paused) return
        paused = false
        segmentStartMs = clockMs()
    }

    /** 停表并返回本次用时快照（秒）。停表后再调用计时方法无效，直至再次 start()。 */
    fun stop(): TimingSnapshot {
        if (running) {
            commitSegment()
            running = false
        }
        val perQuestionSec = accumulatedMs.map { (id, ms) ->
            QuestionTiming(id, msToSec(ms))
        }
        val totalSec = perQuestionSec.sumOf { it.sec }
        val slowest = perQuestionSec.maxByOrNull { it.sec }
        return TimingSnapshot(
            totalSec = totalSec,
            perQuestion = perQuestionSec,
            slowest = slowest
        )
    }

    private fun commitSegment() {
        if (paused) return
        val id = currentId ?: return
        val now = clockMs()
        val delta = (now - segmentStartMs).coerceAtLeast(0)
        accumulatedMs[id] = (accumulatedMs[id] ?: 0) + delta
        segmentStartMs = now
    }

    private fun msToSec(ms: Long): Int = ((ms + 500) / 1000).toInt() // 四舍五入到秒
}

data class QuestionTiming(val id: String, val sec: Int)

data class TimingSnapshot(
    val totalSec: Int,
    val perQuestion: List<QuestionTiming>,
    val slowest: QuestionTiming?
)
