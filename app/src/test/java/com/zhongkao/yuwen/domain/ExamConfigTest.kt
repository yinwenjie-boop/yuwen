package com.zhongkao.yuwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamConfigTest {

    // 取自 exam_config.json 的结构（含异构键 _note / accuracy_threshold）。
    private val sample = """
    {
      "paper": {"total_score": 120},
      "time_baseline": {
        "_note": "说明文字，应被忽略",
        "wenyan_compare": {
          "typical_minutes": [16, 20], "sec_per_score": [64, 80],
          "excellent_max_sec": 960, "source": "x", "confidence": "medium"
        },
        "xiandai_narrative": {
          "typical_minutes": [18, 25], "sec_per_score": [60, 83], "excellent_max_sec": 1200
        },
        "accuracy_threshold": 0.8
      }
    }
    """.trimIndent()

    @Test
    fun `解析出题型基准与达标阈值`() {
        val config = ExamConfig.parse(sample)
        assertEquals(0.8, config.accuracyThreshold, 0.0001)

        val wc = config.baseline(ExamConfig.KEY_WENYAN_COMPARE)
        assertNotNull(wc)
        assertEquals(64.0, wc!!.secPerScore.start, 0.0001)
        assertEquals(80.0, wc.secPerScore.endInclusive, 0.0001)
        assertEquals(960, wc.excellentMaxSec)
        assertEquals(16..20, wc.typicalMinutes)
    }

    @Test
    fun `基准与总分归一化结果与 TimeEvaluator 一致`() {
        val config = ExamConfig.parse(sample)
        // 总分 15 → [64*15, 80*15] = [960, 1200]
        val range = config.baselineRange(ExamConfig.KEY_WENYAN_COMPARE, 15)
        assertEquals(960..1200, range)
    }

    @Test
    fun `未配置的题型返回 null`() {
        val config = ExamConfig.parse(sample)
        assertNull(config.baseline(ExamConfig.KEY_WENYAN_SINGLE))
    }
}
