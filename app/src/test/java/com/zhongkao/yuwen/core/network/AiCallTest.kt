package com.zhongkao.yuwen.core.network

import com.zhongkao.yuwen.data.ai.ChatRequest
import com.zhongkao.yuwen.data.ai.ChatResponse
import com.zhongkao.yuwen.data.ai.Choice
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import com.zhongkao.yuwen.data.ai.ModelOption
import com.zhongkao.yuwen.data.ai.ResponseMessage
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AiCallTest {

    private val req = ChatRequest.build(ModelOption.DEFAULT, "system", "user")

    /** 前 [failTimes] 次抛 [error]，之后返回 [ok]；记录调用次数。 */
    private class FlakyApi(
        val failTimes: Int,
        val error: () -> Throwable,
        val ok: String = "OK"
    ) : DeepSeekApi {
        var calls = 0
        override suspend fun chat(req: ChatRequest): ChatResponse {
            calls++
            if (calls <= failTimes) throw error()
            return ChatResponse(listOf(Choice(ResponseMessage(content = ok))))
        }
    }

    private val noBackoff: suspend (Int) -> Unit = { }

    @Test
    fun `读超时在重试内可恢复`() = runTest {
        val api = FlakyApi(failTimes = 2, error = { SocketTimeoutException("read timed out") })
        val out = api.chatResilient(req, maxRetries = 2, backoff = noBackoff)
        assertEquals("OK", out)
        assertEquals(3, api.calls) // 1 次失败 + 2 次重试成功
    }

    @Test
    fun `超过重试次数仍超时则给超时友好提示`() = runTest {
        val api = FlakyApi(failTimes = 99, error = { SocketTimeoutException() })
        val e = runCatching { api.chatResilient(req, maxRetries = 2, backoff = noBackoff) }.exceptionOrNull()
        assertTrue(e is AiCallException)
        assertTrue((e as AiCallException).friendly.contains("超时"))
        assertEquals(3, api.calls) // 不会无限重试
    }

    @Test
    fun `断网不重试且提示离线可用`() = runTest {
        val api = FlakyApi(failTimes = 99, error = { UnknownHostException("api.deepseek.com") })
        val e = runCatching { api.chatResilient(req, maxRetries = 2, backoff = noBackoff) }.exceptionOrNull()
        assertTrue(e is AiCallException)
        val msg = (e as AiCallException).friendly
        assertTrue(msg.contains("无法联网"))
        assertTrue(msg.contains("错题本"))   // 明确告知离线仍可用
        assertEquals(1, api.calls)            // 非瞬时错误，立即失败不重试
    }

    @Test
    fun `限流可重试鉴权不可重试`() {
        assertTrue(AiErrors.isTransient(httpException(429)))
        assertTrue(AiErrors.isTransient(httpException(503)))
        assertFalse(AiErrors.isTransient(httpException(401)))
        assertFalse(AiErrors.isTransient(UnknownHostException()))
    }

    @Test
    fun `错误分类映射为对应中文提示`() {
        assertTrue(AiErrors.friendlyMessage(httpException(401)).contains("API Key"))
        assertTrue(AiErrors.friendlyMessage(httpException(429)).contains("限流"))
        assertTrue(AiErrors.friendlyMessage(httpException(500)).contains("服务暂时不可用"))
        assertTrue(AiErrors.friendlyMessage(SocketTimeoutException()).contains("超时"))
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))
}
