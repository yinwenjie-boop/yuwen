package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.ChatResponse
import com.zhongkao.yuwen.data.ai.Choice
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.ai.ModelOption
import com.zhongkao.yuwen.data.ai.ResponseMessage
import com.zhongkao.yuwen.data.db.TextBank
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateExerciseUseCaseTest {

    private val localJia = TextBank(
        seedId = "kw_sanxia", category = "课内", title = "三峡", author = "郦道元",
        dynasty = "北魏", text = "【本地真甲文】自三峡七百里中……", sourceRef = "北魏·郦道元《水经注》", verified = true
    )
    private val localYi = TextBank(
        seedId = "kwai_guanning", category = "课外", title = "管宁割席", author = "刘义庆",
        dynasty = "南朝", text = "【本地真乙文】管宁、华歆共园中锄菜……",
        sourceRef = "南朝宋·刘义庆《世说新语》", verified = true
    )

    private class FakePassages(val jia: TextBank?, val yi: TextBank?) : PassageSource {
        override suspend fun randomKewen() = jia
        override suspend fun randomKewai() = yi
    }

    private class FakeCreds(val has: Boolean) : DeepSeekCredentials {
        override fun hasApiKey() = has
        override fun modelOption() = ModelOption.DEFAULT
    }

    private class FakeApi(val content: String) : DeepSeekApi {
        override suspend fun chat(req: ChatRequest): ChatResponse =
            ChatResponse(listOf(Choice(ResponseMessage(content = content))))
    }

    @Test
    fun `文言对比出题用本地原文覆盖被篡改的甲乙`() = runTest {
        // 模型返回的甲/乙原文被故意篡改、出处造假——必须被本地真原文覆盖。
        val tampered = """
        {"type":"wenyan_compare","total_score":4,
         "passage_in":{"title":"假三峡","author":"伪","dynasty":"x","text":"【AI篡改甲】","source_ref":"伪造","verified":true,"need_human_review":false},
         "passage_out":{"title":"假乙","author":"伪","dynasty":"x","text":"【AI篡改乙】","source_ref":"佚名","verified":false,"need_human_review":true},
         "questions":[{"id":"q1","q_type":"句子翻译","stem":"翻译","ref_answer":"译","score_points":[{"point":"关键词","score":4}],"max_score":4,"ability_tag":"句子翻译"}]}
        """.trimIndent()

        val useCase = GenerateExerciseUseCase(
            FakePassages(localJia, localYi), FakeApi(tampered), FakeCreds(has = true)
        )
        val result = useCase.generate(GenerationRequest(type = ExerciseType.WENYAN_COMPARE, totalScore = 4, questionCount = 1))

        assertTrue("应成功而非 $result", result is GenerateExerciseUseCase.Result.Success)
        val gen = (result as GenerateExerciseUseCase.Result.Success).generated
        assertEquals(localJia.text, gen.passageIn?.text)
        assertEquals(localYi.text, gen.passageOut?.text)
        assertEquals(localYi.sourceRef, gen.passageOut?.sourceRef)
        assertEquals(true, gen.passageOut?.verified)          // 本地课外篇 verified=true
        assertEquals("wenyan_compare", gen.type)
    }

    @Test
    fun `无 Key 直接失败`() = runTest {
        val useCase = GenerateExerciseUseCase(
            FakePassages(localJia, localYi), FakeApi("{}"), FakeCreds(has = false)
        )
        val result = useCase.generate(GenerationRequest(type = ExerciseType.WENYAN_COMPARE))
        assertTrue(result is GenerateExerciseUseCase.Result.Failure)
    }

    @Test
    fun `课外库为空时文言对比失败`() = runTest {
        val useCase = GenerateExerciseUseCase(
            FakePassages(localJia, null), FakeApi("{}"), FakeCreds(has = true)
        )
        val result = useCase.generate(GenerationRequest(type = ExerciseType.WENYAN_COMPARE))
        assertTrue(result is GenerateExerciseUseCase.Result.Failure)
    }

    @Test
    fun `现代文返回空题目应判为校验不通过`() = runTest {
        val noQuestions = """
        {"type":"xiandai","total_score":25,
         "passage":{"title":"原创","author":"","dynasty":"","text":"一段原创短文……","source_ref":"原创","verified":true,"need_human_review":false},
         "questions":[]}
        """.trimIndent()
        val useCase = GenerateExerciseUseCase(
            FakePassages(null, null), FakeApi(noQuestions), FakeCreds(has = true)
        )
        val result = useCase.generate(GenerationRequest(type = ExerciseType.XIANDAI, totalScore = 25))
        assertTrue("应为 Invalid 而非 $result", result is GenerateExerciseUseCase.Result.Invalid)
    }
}
