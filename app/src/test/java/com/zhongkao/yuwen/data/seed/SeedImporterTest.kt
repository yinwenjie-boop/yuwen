package com.zhongkao.yuwen.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedImporterTest {

    private val sample = """
    {
      "_meta": {"about": "测试样本"},
      "items": [
        {
          "id": "kw_a", "category": "课内", "title": "甲篇", "author": "X", "dynasty": "唐",
          "grade": "八上", "theme": "写景", "ability_focus": ["实词", "写景手法"],
          "text": "原文甲……", "notes": [{"term": "馨", "explain": "香气"}],
          "translation": "译文甲", "source_ref": "唐·X《甲篇》", "verified": true
        },
        {
          "id": "kw_blank", "category": "课内", "title": "空原文篇",
          "text": "", "verified": true, "source_ref": "出处"
        },
        {
          "id": "kw_unverified", "category": "课内", "title": "未确认篇",
          "text": "有原文", "verified": false, "source_ref": "出处"
        },
        {
          "id": "kwai_ok", "category": "课外", "title": "课外真文",
          "text": "课外原文", "verified": true, "need_human_review": false,
          "source_ref": "南朝宋·刘义庆《世说新语》", "link_hint": "可对比陋室铭"
        },
        {
          "id": "kwai_review", "category": "课外", "title": "待人工确认篇",
          "text": "课外原文", "verified": true, "need_human_review": true,
          "source_ref": "某出处"
        },
        {
          "id": "kwai_nosrc", "category": "课外", "title": "缺出处篇",
          "text": "课外原文", "verified": true, "need_human_review": false, "source_ref": ""
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `防伪过滤只放行合规篇目`() {
        val result = SeedImporter.import(sample)
        val ids = result.importable.map { it.seedId }.toSet()
        assertEquals(setOf("kw_a", "kwai_ok"), ids)
        assertEquals(1, result.kewenCount)
        assertEquals(1, result.kewaiCount)
    }

    @Test
    fun `被拦截篇目都带明确原因`() {
        val result = SeedImporter.import(sample)
        val byId = result.skipped.associateBy { it.id }
        assertTrue(byId["kw_blank"]!!.reason.contains("原文为空"))
        assertTrue(byId["kw_unverified"]!!.reason.contains("verified=false"))
        assertTrue(byId["kwai_review"]!!.reason.contains("人工确认"))
        assertTrue(byId["kwai_nosrc"]!!.reason.contains("出处"))
        assertEquals(4, result.skipped.size)
    }

    @Test
    fun `合规课内篇正确映射字段且 notes 序列化为 JSON`() {
        val item = SeedImporter.parse(sample).first { it.id == "kw_a" }
        assertNull(SeedImporter.rejectionReason(item))
        val entity = SeedImporter.toEntity(item)
        assertEquals("kw_a", entity.seedId)
        assertEquals("课内", entity.category)
        assertEquals("八上", entity.grade)
        assertTrue(entity.notes.contains("\"term\""))
        assertTrue(entity.abilityFocus.contains("实词"))
        assertTrue(entity.verified)
    }
}
