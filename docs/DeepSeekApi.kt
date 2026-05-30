package com.zhongkao.yuwen.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * DeepSeek 接入（OpenAI 兼容）。base_url = "https://api.deepseek.com/"
 * 默认 deepseek-v4-pro 思考模式；只取 message.content 作答案，忽略 reasoning_content。
 * ⚠️ thinking / reasoning_effort 字段名以 DeepSeek 官方 thinking_mode 文档为准，接入时核对一次。
 */
interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chat(@Body req: ChatRequest): ChatResponse
}

/** 设置页下拉菜单的四个选项；默认 PRO_THINK。 */
enum class ModelOption(val display: String, val model: String, val thinking: Boolean) {
    PRO_THINK("deepseek-v4-pro · 思考模式（默认）", "deepseek-v4-pro", true),
    PRO_PLAIN("deepseek-v4-pro · 非思考", "deepseek-v4-pro", false),
    FLASH_THINK("deepseek-v4-flash · 思考模式", "deepseek-v4-flash", true),
    FLASH_PLAIN("deepseek-v4-flash · 非思考（省钱更快）", "deepseek-v4-flash", false);

    companion object { val DEFAULT = PRO_THINK }
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    @SerialName("response_format") val responseFormat: ResponseFormat? = ResponseFormat("json_object"),
    val thinking: Thinking? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null
) {
    companion object {
        /** 用设置页选项 + system/user 文本组装一次请求。思考模式不传 temperature（无效）。 */
        fun build(opt: ModelOption, system: String, user: String): ChatRequest = ChatRequest(
            model = opt.model,
            messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
            thinking = Thinking(if (opt.thinking) "enabled" else "disabled"),
            reasoningEffort = if (opt.thinking) "high" else null,
            temperature = if (opt.thinking) null else 0.7
        )
    }
}

@Serializable data class ChatMessage(val role: String, val content: String)
@Serializable data class ResponseFormat(val type: String)
@Serializable data class Thinking(val type: String) // "enabled" | "disabled"

@Serializable
data class ChatResponse(val choices: List<Choice> = emptyList()) {
    /** 取最终答案，忽略思维链。 */
    fun answer(): String = choices.firstOrNull()?.message?.content.orEmpty()
}
@Serializable data class Choice(val message: ResponseMessage)
@Serializable data class ResponseMessage(
    val role: String = "assistant",
    val content: String = "",
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

/** 反序列化前清洗：截取首个 '{' 到最后一个 '}'，去掉模型偶尔多带的杂字。 */
fun extractJson(raw: String): String {
    val s = raw.indexOf('{'); val e = raw.lastIndexOf('}')
    return if (s >= 0 && e > s) raw.substring(s, e + 1) else raw
}

/** 鉴权：key 取自 EncryptedSharedPreferences，勿打印、勿上传。 */
class AuthInterceptor(private val keyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${keyProvider()}")
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(req)
    }
}
