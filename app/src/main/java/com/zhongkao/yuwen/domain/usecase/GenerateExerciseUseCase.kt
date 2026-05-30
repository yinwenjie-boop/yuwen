package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.ai.AiResponseValidator
import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.ai.GeneratedExercise
import com.zhongkao.yuwen.data.ai.ModelOption
import com.zhongkao.yuwen.data.ai.extractJson
import com.zhongkao.yuwen.data.ai.toPassage
import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.domain.prompt.Prompts

/** 出题选篇来源（由 TextBankRepository 实现；便于单测注入假数据）。 */
interface PassageSource {
    suspend fun randomKewen(): TextBank?
    suspend fun randomKewai(): TextBank?
}

/** DeepSeek 凭据/模型选择（由 SecureKeyStore 实现；不暴露明文 Key）。 */
interface DeepSeekCredentials {
    fun hasApiKey(): Boolean
    fun modelOption(): ModelOption
}

/**
 * 出题用例（SPEC §2 防伪 + §6 校验）。
 * 流程：选篇 → 拼提示词 → 调 DeepSeek → extractJson → 反序列化 →
 *       【防伪】用本地真实原文覆盖文言甲/乙 → AiResponseValidator 校验。
 */
class GenerateExerciseUseCase(
    private val textBank: PassageSource,
    private val api: DeepSeekApi,
    private val credentials: DeepSeekCredentials
) {
    sealed interface Result {
        /** 可用且可入正式库。 */
        data class Success(
            val generated: GeneratedExercise,
            val snapshotJson: String,
            val warnings: List<String>
        ) : Result

        /** 内容结构可用但需人工确认（课外篇未确认）；不入正式库。 */
        data class NeedsReview(val warnings: List<String>) : Result

        /** 校验硬错误，应提示重试。 */
        data class Invalid(val errors: List<String>) : Result

        /** 流程失败：无 Key / 选篇为空 / 网络 / 解析失败。 */
        data class Failure(val message: String) : Result
    }

    suspend fun generate(req: GenerationRequest): Result {
        if (!credentials.hasApiKey()) {
            return Result.Failure("尚未设置 DeepSeek API Key，请到设置页填写后再出题。")
        }

        val system: String
        val user: String
        var jia: TextBank? = null
        var yi: TextBank? = null

        when (req.type) {
            ExerciseType.WENYAN_COMPARE -> {
                jia = textBank.randomKewen()
                    ?: return Result.Failure("课内语料库为空，无法出文言对比题。")
                yi = textBank.randomKewai()
                    ?: return Result.Failure("课外语料库为空，无法选乙文。")
                system = Prompts.WENYAN_COMPARE_SYSTEM
                user = Prompts.wenyanCompareUser(jia, yi, req)
            }
            ExerciseType.XIANDAI -> {
                system = Prompts.xiandaiSystem(req)
                user = Prompts.xiandaiUser(req)
            }
        }

        val raw = try {
            api.chat(ChatRequest.build(credentials.modelOption(), system, user)).answer()
        } catch (e: Exception) {
            return Result.Failure("调用 DeepSeek 失败：${e.message ?: "网络错误"}，请检查网络与余额后重试。")
        }
        if (raw.isBlank()) return Result.Failure("AI 未返回内容，请重试。")

        val parsed = try {
            AppJson.decodeFromString(GeneratedExercise.serializer(), extractJson(raw))
        } catch (e: Exception) {
            return Result.Failure("AI 返回不是合法 JSON，请重试。")
        }

        // 防伪加固：文言甲/乙强制使用本地真实原文，AI 仅保留出题。
        val fixed = when (req.type) {
            ExerciseType.WENYAN_COMPARE -> parsed.copy(
                type = req.type.apiType,
                passageIn = jia!!.toPassage(),
                passageOut = yi!!.toPassage()
            )
            ExerciseType.XIANDAI -> parsed.copy(type = req.type.apiType)
        }

        val report = AiResponseValidator.validateGenerated(fixed)
        return when {
            !report.isUsable -> Result.Invalid(report.errors)
            report.needsReview -> Result.NeedsReview(report.warnings)
            else -> Result.Success(
                generated = fixed,
                snapshotJson = AppJson.encodeToString(GeneratedExercise.serializer(), fixed),
                warnings = report.warnings
            )
        }
    }
}
