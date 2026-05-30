package com.zhongkao.yuwen.core.network

import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 出题/批改调用失败时携带「对用户友好的中文提示」。
 * message 即可直接展示给学生，不含任何技术堆栈/Key。
 */
class AiCallException(val friendly: String, cause: Throwable?) : Exception(friendly, cause)

/**
 * DeepSeek 调用加固（阶段 5 · 健壮性）：
 *  - 瞬时故障（读超时 / 429 限流 / 5xx）自动退避重试，默认最多 [maxRetries] 次；
 *  - 最终失败按异常类型映射为友好中文提示（断网 / 超时 / 限流 / 鉴权 / 服务端）；
 *  - 不打印任何请求内容与 Key（沿用 NetworkModule 的 redact 策略）。
 *
 * [backoff] 抽出以便单测注入零延迟；生产用指数退避。
 */
suspend fun DeepSeekApi.chatResilient(
    req: ChatRequest,
    maxRetries: Int = 2,
    backoff: suspend (attempt: Int) -> Unit = { attempt -> delay(700L * attempt) }
): String {
    var attempt = 0
    while (true) {
        try {
            return chat(req).answer()
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e // 不吞协程取消
            if (AiErrors.isTransient(e) && attempt < maxRetries) {
                attempt++
                backoff(attempt)
                continue
            }
            throw AiCallException(AiErrors.friendlyMessage(e), e)
        }
    }
}

/** 异常 → 是否可重试 / 友好提示 的纯逻辑（便于单测，不依赖网络）。 */
object AiErrors {

    /** 仅这些 HTTP 码算「瞬时」可重试：限流、网关/服务端错误。 */
    private val TRANSIENT_HTTP = setOf(429, 500, 502, 503, 504)

    fun isTransient(e: Throwable): Boolean = when (e) {
        is SocketTimeoutException -> true
        is HttpException -> isTransientHttp(e.code())
        else -> false
    }

    /** 按 HTTP 状态码判断是否可重试（纯逻辑，便于单测）。 */
    fun isTransientHttp(code: Int): Boolean = code in TRANSIENT_HTTP

    fun friendlyMessage(e: Throwable): String = when (e) {
        // 断网/无法解析主机：本地题库、错题本、薄弱点仍可用，明确告知。
        is UnknownHostException, is ConnectException ->
            "当前无法联网，AI 出题/批改需要网络。题库、错题本、薄弱点仍可离线查看；请联网后重试。"
        is SocketTimeoutException ->
            "请求超时（已自动重试）。思考模式较慢，请稍后重试，或在设置页改用更快的「非思考」模型。"
        is HttpException -> httpMessage(e.code())
        is IOException ->
            "网络异常：${e.message ?: "连接中断"}。请检查网络后重试。"
        else ->
            "调用 DeepSeek 失败：${e.message ?: "未知错误"}。请检查网络与余额后重试。"
    }

    /** 按 HTTP 状态码给友好提示（纯逻辑，便于单测）。 */
    fun httpMessage(code: Int): String = when (code) {
        401, 403 -> "API Key 无效或无权限，请到设置页检查 DeepSeek API Key 是否正确。"
        429 -> "请求过于频繁（限流），已自动重试仍失败，请稍候片刻再试。"
        in 500..599 -> "DeepSeek 服务暂时不可用（已重试），请稍后再试。"
        else -> "DeepSeek 返回错误（HTTP $code），请稍后重试。"
    }
}
