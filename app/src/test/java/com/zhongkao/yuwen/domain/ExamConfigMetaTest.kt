package com.zhongkao.yuwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamConfigMetaTest {

    private val pendingJson = """
    {
      "_meta": {
        "official_doc_status": "PENDING_2026",
        "region": "常州市",
        "exam_year": 2026,
        "calibration_instruction": ["1) 拿到命题说明后替换 estimated 字段", "2) source 改 official_2026", "3) 篇目以官方为准"]
      },
      "time_baseline": {
        "wenyan_compare": {"typical_minutes": [16,20], "sec_per_score": [64,80], "excellent_max_sec": 960},
        "accuracy_threshold": 0.8
      }
    }
    """.trimIndent()

    @Test
    fun `PENDING 状态视为未校准并保留校准指引`() {
        val cfg = ExamConfig.parse(pendingJson)
        assertFalse(cfg.meta.isCalibrated)
        assertEquals("常州市", cfg.meta.region)
        assertEquals(2026, cfg.meta.examYear)
        assertEquals(3, cfg.meta.calibrationInstruction.size)
        // 基准仍可正常解析供时间评价使用。
        assertEquals(0.8, cfg.accuracyThreshold, 0.0001)
    }

    @Test
    fun `官方状态视为已校准`() {
        val calibrated = pendingJson.replace("PENDING_2026", "official_2026")
        val cfg = ExamConfig.parse(calibrated)
        assertTrue(cfg.meta.isCalibrated)
    }

    @Test
    fun `缺省meta不报错且按未校准处理`() {
        val noMeta = """
        {"time_baseline": {"accuracy_threshold": 0.8}}
        """.trimIndent()
        val cfg = ExamConfig.parse(noMeta)
        assertFalse(cfg.meta.isCalibrated)
        assertTrue(cfg.meta.calibrationInstruction.isEmpty())
    }
}
