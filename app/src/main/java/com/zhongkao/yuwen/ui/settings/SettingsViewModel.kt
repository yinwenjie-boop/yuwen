package com.zhongkao.yuwen.ui.settings

import androidx.lifecycle.ViewModel
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.data.ai.ModelOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val apiKeyInput: String = "",
    val hasSavedKey: Boolean = false,
    val selectedModel: ModelOption = ModelOption.DEFAULT,
    val savedTick: Int = 0          // 自增以触发"已保存"提示
)

/**
 * 设置页 VM：API Key + 模型下拉。Key 只进 SecureKeyStore，UI 不回显已存明文。
 */
class SettingsViewModel(private val keyStore: SecureKeyStore) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            hasSavedKey = keyStore.hasApiKey(),
            selectedModel = keyStore.getModelOption()
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val modelOptions: List<ModelOption> = ModelOption.entries

    fun onApiKeyChange(value: String) {
        _state.update { it.copy(apiKeyInput = value) }
    }

    fun onModelSelected(option: ModelOption) {
        keyStore.setModelOption(option)
        _state.update { it.copy(selectedModel = option) }
    }

    fun save() {
        val input = _state.value.apiKeyInput
        if (input.isNotBlank()) {
            keyStore.setApiKey(input)
        }
        _state.update {
            it.copy(
                apiKeyInput = "",               // 存完即清空输入框，不在内存/UI 久留
                hasSavedKey = keyStore.hasApiKey(),
                savedTick = it.savedTick + 1
            )
        }
    }
}
