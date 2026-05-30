package com.zhongkao.yuwen.core.network

import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.data.ai.AuthInterceptor
import com.zhongkao.yuwen.data.ai.DeepSeekApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络层装配：Retrofit + OkHttp + Kotlinx Serialization → DeepSeek。
 * base_url = https://api.deepseek.com/（OpenAI 兼容）。
 * 鉴权 Key 由 SecureKeyStore 提供，AuthInterceptor 按请求注入，绝不打印。
 */
object NetworkModule {

    private const val BASE_URL = "https://api.deepseek.com/"

    fun createDeepSeekApi(keyStore: SecureKeyStore): DeepSeekApi {
        val contentType = "application/json".toMediaType()

        // 日志拦截器只记 HEADERS，且不打印 Authorization 头，避免 Key 泄露。
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(keyProvider = { keyStore.getApiKey() }))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            // 思考模式响应较慢，读超时放宽。
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory(contentType))
            .build()
            .create(DeepSeekApi::class.java)
    }
}
