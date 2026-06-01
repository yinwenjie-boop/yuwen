package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.data.ai.GradingResult
import com.zhongkao.yuwen.domain.ExamConfig
import com.zhongkao.yuwen.domain.TimeEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementCalculatorTest {

    private val config = ExamConfig.parse(
        """
        {"time_baseline":{
          "wenyan_compare":{"typical_minutes":[16,20],"sec_per_score":[64,80],"excellent_max_sec":960},
          "xiandai_narrative":{"typical_minutes":[18,25],"sec_per_score":[60,83],"excellent_max_sec":1200},
          "accuracy_threshold":0.8}}
        """.trimIndent()
    )

    private fun grading(got: Int, full: Int) = GradingResult(
        perQuestion = emptyList(), totalGot = got, totalFull = full,
        weakPoints = emptyList(), nextAdvice = ""
    )

    @Test
    fun `文言对比又快又准合成正确`() {
        val settlement = SettlementCalculator.build(
            apiType = "wenyan_compare",
            genre = null,
            totalScore = 15,
            totalTimeSec = 1000,                 // ≤ 基准上限 1200
            perQuestionSec = mapOf("q1" to 100, "q2" to 300, "q3" to 600),
            grading = grading(got = 12, full = 15),  // 0.8 达标
            examConfig = config
        )
        assertEquals(0.8, settlement.accuracy, 0.0001)
        assertEquals(listOf(960, 1200), settlement.baselineRange)
        assertEquals(TimeEvaluator.Quadrant.FAST_ACCURATE.label, settlement.timeRating)
        assertEquals("q3", settlement.slowestQuestion?.id)
        assertEquals(600, settlement.slowestQuestion?.sec)
    }

    @Test
    fun `超时且正确率低为基础待巩固`() {
        val settlement = SettlementCalculator.build(
            apiType = "xiandai",
            genre = "记叙文",                       // → xiandai_narrative
            totalScore = 20,
            totalTimeSec = 3000,                  // 远超基准上限 83*20=1660
            perQuestionSec = mapOf("q1" to 3000),
            grading = grading(got = 8, full = 20), // 0.4 不达标
            examConfig = config
        )
        assertEquals(TimeEvaluator.Quadrant.SLOW_INACCURATE.label, settlement.timeRating)
    }

    @Test
    fun `说明文映射到议论说明基准键`() {
        assertEquals(ExamConfig.KEY_XIANDAI_EXPOSITORY, SettlementCalculator.baselineKey("xiandai", "说明文"))
        assertEquals(ExamConfig.KEY_XIANDAI_NARRATIVE, SettlementCalculator.baselineKey("xiandai", "散文"))
        assertEquals(ExamConfig.KEY_WENYAN_COMPARE, SettlementCalculator.baselineKey("wenyan_compare", null))
    }

    @Test
    fun `小说归文学类基准 非连续性文本归实用类基准`() {
        // 小说与记叙/散文同属文学类 → narrative
        assertEquals(ExamConfig.KEY_XIANDAI_NARRATIVE, SettlementCalculator.baselineKey("xiandai", "小说"))
        // 非连续性文本属实用/论述类 → expository
        assertEquals(ExamConfig.KEY_XIANDAI_EXPOSITORY, SettlementCalculator.baselineKey("xiandai", "非连续性文本"))
    }
}
