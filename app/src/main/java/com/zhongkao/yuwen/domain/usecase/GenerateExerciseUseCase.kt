package com.zhongkao.yuwen.domain.usecase

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.core.network.AiCallException
import com.zhongkao.yuwen.core.network.chatResilient
import com.zhongkao.yuwen.data.ai.AiResponseValidator
import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.ai.GeneratedExercise
import com.zhongkao.yuwen.data.ai.ModelOption
import com.zhongkao.yuwen.data.ai.Passage
import com.zhongkao.yuwen.data.ai.extractJson
import com.zhongkao.yuwen.data.ai.toPassage
import com.zhongkao.yuwen.data.db.TextBank
import com.zhongkao.yuwen.domain.prompt.Prompts

/** 出题选篇来源（由 TextBankRepository 实现；便于单测注入假数据）。 */
interface PassageSource {
    suspend fun randomKewen(): TextBank?
    suspend fun randomKewai(): TextBank?

    /** AI 提议的课外篇存入「待人工确认」（verified=false），返回行 id。 */
    suspend fun savePendingKewai(passage: Passage): Long
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

        /**
         * 内容结构可用但需人工确认（AI 提议课外篇未确认）；不入正式题库。
         * 若 [pendingTitle] 非空，表示该课外篇已存入「待确认」，可到确认页处理。
         */
        data class NeedsReview(
            val warnings: List<String>,
            val pendingTitle: String? = null
        ) : Result

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
                // 课外篇优先选库；库内无已确认篇目时，才走 AI 提议兜底（A2.2），结果须人工确认。
                yi = textBank.randomKewai()
                if (yi != null) {
                    system = Prompts.WENYAN_COMPARE_SYSTEM
                    user = Prompts.wenyanCompareUser(jia, yi, req)
                } else {
                    system = Prompts.WENYAN_COMPARE_SYSTEM + "\n" + Prompts.WENYAN_PROPOSE_APPENDIX
                    user = Prompts.wenyanProposeUser(jia, req)
                }
            }
            ExerciseType.XIANDAI -> {
                system = Prompts.xiandaiSystem(req)
                user = Prompts.xiandaiUser(req)
            }
        }

        val raw = try {
            api.chatResilient(ChatRequest.build(credentials.modelOption(), system, user))
        } catch (e: AiCallException) {
            return Result.Failure(e.friendly)
        }
        if (raw.isBlank()) return Result.Failure("AI 未返回内容，请重试。")

        // 解析失败兜底：清洗后仍非合法 JSON 不写库，给可重试的友好提示。
        val parsed = try {
            AppJson.decodeFromString(GeneratedExercise.serializer(), extractJson(raw))
        } catch (e: Exception) {
            return Result.Failure("AI 返回不是合法 JSON，已拦截不入库，请重试。")
        }

        // 防伪加固：文言甲（课内）强制用本地真实原文；乙文若来自本地库同样覆盖，
        // 若为 AI 提议（yi==null）则保留 AI 返回的乙文交由人工确认，绝不自动入库。
        val fixed = when (req.type) {
            ExerciseType.WENYAN_COMPARE ->
                if (yi != null) parsed.copy(
                    type = req.type.apiType,
                    passageIn = jia!!.toPassage(),
                    passageOut = yi.toPassage()
                ) else parsed.copy(
                    type = req.type.apiType,
                    passageIn = jia!!.toPassage(),
                    // 无本地核实手段：强制乙文为未确认，杜绝 AI 谎称 verified=true 而绕过人工闸门。
                    passageOut = parsed.passageOut?.copy(verified = false, needHumanReview = true)
                )
            ExerciseType.XIANDAI -> parsed.copy(type = req.type.apiType)
        }

        val report = AiResponseValidator.validateGenerated(fixed)
        return when {
            !report.isUsable -> Result.Invalid(report.errors)
            report.needsReview -> {
                // AI 提议课外篇：有可读乙文则存入「待确认」，确认真实出处后方可用于出题。
                val proposed = fixed.passageOut
                val pendingTitle = if (proposed != null && proposed.text.isNotBlank()) {
                    runCatching { textBank.savePendingKewai(proposed) }
                    proposed.title.ifBlank { "未命名课外篇" }
                } else null
                Result.NeedsReview(
                    warnings = report.warnings + listOfNotNull(
                        pendingTitle?.let { "已将课外篇《$it》存入「待确认」，确认真实出处后即可用于出题。" }
                    ),
                    pendingTitle = pendingTitle
                )
            }
            else -> Result.Success(
                generated = fixed,
                snapshotJson = AppJson.encodeToString(GeneratedExercise.serializer(), fixed),
                warnings = report.warnings
            )
        }
    }
}
