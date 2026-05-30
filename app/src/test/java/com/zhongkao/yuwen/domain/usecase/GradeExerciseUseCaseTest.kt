package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.ChatResponse
import com.zhongkao.yuwen.data.ai.Choice
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.ai.GenQuestion
import com.zhongkao.yuwen.data.ai.ModelOption
import com.zhongkao.yuwen.data.ai.ResponseMessage
import com.zhongkao.yuwen.data.ai.ScorePoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeExerciseUseCaseTest {

    private class FakeCreds(val has: Boolean) : DeepSeekCredentials {
        override fun hasApiKey() = has
        override fun modelOption() = ModelOption.DEFAULT
    }

    private class FakeApi(val content: String) : DeepSeekApi {
        override suspend fun chat(req: ChatRequest): ChatResponse =
            ChatResponse(listOf(Choice(ResponseMessage(content = content))))
    }

    private val questions = listOf(
        GenQuestion(
            id = "q1", qType = "句子翻译", stem = "翻译句子", refAnswer = "参考译文",
            scorePoints = listOf(ScorePoint("关键词", 4)), maxScore = 4, abilityTag = "句子翻译"
        )
    )
    private val answers = listOf(StudentAnswerDto("q1", "我的翻译"))

    @Test
    fun `批改返回合法且题号匹配则成功`() = runTest {
        val json = """
        {"per_question":[{"id":"q1","got_score":2,"max_score":4,
          "point_check":[{"point":"关键词","hit":true,"score":2}],
          "error_type":"漏译关键词","correct_answer":"规范译文","explanation":"...","tip":"复习实词"}],
         "total_got":2,"total_full":4,"weak_points":["句子翻译"],"next_advice":"多练翻译"}
        """.trimIndent()
        val useCase = GradeExerciseUseCase(FakeApi(json), FakeCreds(has = true))
        val result = useCase.grade(questions, answers, expectedIds = setOf("q1"))
        assertTrue("应成功而非 $result", result is GradeExerciseUseCase.Result.Success)
        val g = (result as GradeExerciseUseCase.Result.Success).grading
        assertEquals(2, g.totalGot)
        assertEquals("规范译文", g.perQuestion.single().correctAnswer)
    }

    @Test
    fun `批改题号与出题不一致则校验拦截`() = runTest {
        val json = """
        {"per_question":[{"id":"q1","got_score":2,"max_score":4,"point_check":[],"error_type":"","correct_answer":"","explanation":"x","tip":"y"}],
         "total_got":2,"total_full":4,"weak_points":[],"next_advice":""}
        """.trimIndent()
        val useCase = GradeExerciseUseCase(FakeApi(json), FakeCreds(has = true))
        val result = useCase.grade(questions, answers, expectedIds = setOf("q1", "q2"))
        assertTrue("应为 Invalid 而非 $result", result is GradeExerciseUseCase.Result.Invalid)
    }

    @Test
    fun `无 Key 直接失败`() = runTest {
        val useCase = GradeExerciseUseCase(FakeApi("{}"), FakeCreds(has = false))
        val result = useCase.grade(questions, answers, expectedIds = setOf("q1"))
        assertTrue(result is GradeExerciseUseCase.Result.Failure)
    }
}
