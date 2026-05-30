package com.zhongkao.yuwen.domain.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyProfileTest {

    @Test
    fun `四档难度齐全且字数递增`() {
        assertEquals(listOf("基础", "中等", "偏难", "拔尖"), DifficultyProfile.LABELS)
        val words = DifficultyProfile.ALL.map { it.xiandaiWords }
        assertEquals(words.sorted(), words) // 单调不减
        assertTrue(DifficultyProfile.BASIC.xiandaiWords < DifficultyProfile.TOP.xiandaiWords)
    }

    @Test
    fun `查表命中与未知值回退中等`() {
        assertSame(DifficultyProfile.TOP, DifficultyProfile.of("拔尖"))
        assertSame(DifficultyProfile.BASIC, DifficultyProfile.of("基础"))
        assertSame(DifficultyProfile.MEDIUM, DifficultyProfile.of("不存在的难度"))
        assertSame(DifficultyProfile.MEDIUM, DifficultyProfile.of(""))
    }
}
