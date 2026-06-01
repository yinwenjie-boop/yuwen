package com.zhongkao.yuwen.domain.prompt

import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.domain.usecase.ExerciseType
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptsTest {

    private val jia = TextBank(
        seedId = "kw_sanxia", category = "课内", title = "三峡", author = "郦道元",
        dynasty = "北魏", text = "自三峡七百里中……", sourceRef = "北魏·郦道元《水经注》", verified = true
    )
    private val yi = TextBank(
        seedId = "kwai_x", category = "课外", title = "管宁割席", author = "刘义庆",
        dynasty = "南朝", text = "管宁、华歆共园中锄菜……", sourceRef = "南朝宋·刘义庆《世说新语》", verified = true
    )

    @Test
    fun `文言对比 user 含甲乙原文与出处与总分要求`() {
        val req = GenerationRequest(type = ExerciseType.WENYAN_COMPARE, totalScore = 15, questionCount = 4, focus = "虚词之")
        val user = Prompts.wenyanCompareUser(jia, yi, req)
        assertTrue(user.contains("三峡"))
        assertTrue(user.contains("自三峡七百里中"))
        assertTrue(user.contains("管宁割席"))
        assertTrue(user.contains("南朝宋·刘义庆《世说新语》"))
        assertTrue(user.contains("total_score=15"))
        assertTrue(user.contains("侧重考点=虚词之"))
    }

    @Test
    fun `文言对比 user 注入对应难度的硬性细则`() {
        val req = GenerationRequest(type = ExerciseType.WENYAN_COMPARE, difficulty = "拔尖")
        val user = Prompts.wenyanCompareUser(jia, yi, req)
        assertTrue(user.contains("难度=拔尖"))
        assertTrue(user.contains("【难度要求·拔尖】"))
        assertTrue(user.contains("压轴")) // 拔尖细则关键词
        assertTrue(user.contains("探究"))
    }

    @Test
    fun `现代文 system 注入对应难度的硬性细则`() {
        val req = GenerationRequest(type = ExerciseType.XIANDAI, difficulty = "拔尖", genre = "记叙文")
        val system = Prompts.xiandaiSystem(req)
        assertTrue(system.contains("【难度要求·拔尖】"))
        assertTrue(system.contains("探究"))
    }

    @Test
    fun `现代文 system 按文体注入对应题型要点`() {
        val xiaoshuo = Prompts.xiandaiSystem(
            GenerationRequest(type = ExerciseType.XIANDAI, genre = "小说", targetWords = 1000)
        )
        assertTrue(xiaoshuo.contains("【文体要点·小说】"))
        assertTrue(xiaoshuo.contains("情节"))
        assertTrue(xiaoshuo.contains("环境描写"))

        val feilianxu = Prompts.xiandaiSystem(
            GenerationRequest(type = ExerciseType.XIANDAI, genre = "非连续性文本", targetWords = 700)
        )
        assertTrue(feilianxu.contains("【文体要点·非连续性文本】"))
        assertTrue(feilianxu.contains("材料一"))
        assertTrue(feilianxu.contains("图表"))
    }

    @Test
    fun `现代文 system 含文体字数与 xiandai 结构`() {
        val req = GenerationRequest(type = ExerciseType.XIANDAI, totalScore = 25, genre = "散文", targetWords = 900)
        val system = Prompts.xiandaiSystem(req)
        assertTrue(system.contains("散文"))
        assertTrue(system.contains("约900字"))
        assertTrue(system.contains("\"type\":\"xiandai\""))
        assertTrue(system.contains("\"total_score\":25"))

        val user = Prompts.xiandaiUser(req)
        assertTrue(user.contains("文体=散文"))
        assertTrue(user.contains("字数≈900"))
    }
}
