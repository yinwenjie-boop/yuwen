package com.zhongkao.yuwen.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zhongkao.yuwen.data.ai.ModelOption

/**
 * API Key 与模型选项的安全存取（SPEC 硬约束 4）。
 *  - 用 EncryptedSharedPreferences 加密落盘；
 *  - 绝不硬编码、绝不打印、绝不上传；本类不提供任何日志输出。
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getApiKey(): String = prefs.getString(KEY_API, "").orEmpty()

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API, value.trim()).apply()
    }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    fun getModelOption(): ModelOption {
        val name = prefs.getString(KEY_MODEL, null)
        return runCatching { ModelOption.valueOf(name!!) }.getOrDefault(ModelOption.DEFAULT)
    }

    fun setModelOption(option: ModelOption) {
        prefs.edit().putString(KEY_MODEL, option.name).apply()
    }

    private companion object {
        const val PREF_FILE = "secure_settings"
        const val KEY_API = "deepseek_api_key"
        const val KEY_MODEL = "deepseek_model_option"
    }
}
