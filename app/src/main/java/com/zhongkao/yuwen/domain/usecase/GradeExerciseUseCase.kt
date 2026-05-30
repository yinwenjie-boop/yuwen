package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.ai.AiResponseValidator
import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.ai.GenQuestion
import com.zhongkao.yuwen.data.ai.GradingResult
import com.zhongkao.yuwen.data.ai.extractJson
import com.zhongkao.yuwen.domain.prompt.Prompts
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** 学生作答（拼入批改 user 消息）。 */
@Serializable
data class StudentAnswerDto(
    @SerialName("id") val id: String,
    @SerialName("answer") val answer: String
)

/**
 * 批改用例（SPEC §2 踩点给分 + §6 校验）。
 * 流程：拼 A2.4 提示词 → 调 DeepSeek → extractJson → 反序列化 GradingResult →
 *       validateGrading(expectedIds=本次题目id集合)。
 */
class GradeExerciseUseCase(
    private val api: DeepSeekApi,
    private val credentials: DeepSeekCredentials
) {
    sealed interface Result {
        data class Success(val grading: GradingResult, val warnings: List<String>) : Result
        data class Invalid(val errors: List<String>) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun grade(
        questions: List<GenQuestion>,
        answers: List<StudentAnswerDto>,
        expectedIds: Set<String>
    ): Result {
        if (!credentials.hasApiKey()) {
            return Result.Failure("尚未设置 DeepSeek API Key，请到设置页填写后再批改。")
        }

        val questionsJson = AppJson.encodeToString(ListSerializer(GenQuestion.serializer()), questions)
        val answersJson = AppJson.encodeToString(ListSerializer(StudentAnswerDto.serializer()), answers)
        val user = Prompts.gradingUser(questionsJson, answersJson)

        val raw = try {
            api.chat(ChatRequest.build(credentials.modelOption(), Prompts.GRADING_SYSTEM, user)).answer()
        } catch (e: Exception) {
            return Result.Failure("调用 DeepSeek 失败：${e.message ?: "网络错误"}，请检查网络与余额后重试。")
        }
        if (raw.isBlank()) return Result.Failure("AI 未返回内容，请重试。")

        val grading = try {
            AppJson.decodeFromString(GradingResult.serializer(), extractJson(raw))
        } catch (e: Exception) {
            return Result.Failure("批改返回不是合法 JSON，请重试。")
        }

        val report = AiResponseValidator.validateGrading(grading, expectedIds)
        return if (!report.isUsable) Result.Invalid(report.errors)
        else Result.Success(grading, report.warnings)
    }
}
