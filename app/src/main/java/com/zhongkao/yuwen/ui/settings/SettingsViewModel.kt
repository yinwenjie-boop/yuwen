package com.zhongkao.yuwen.ui.settings

import androidx.lifecycle.ViewModel
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.core.settings.IncentiveSettingsStore
import com.zhongkao.yuwen.data.ai.ModelOption
import com.zhongkao.yuwen.domain.incentive.IncentiveConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val apiKeyInput: String = "",
    val hasSavedKey: Boolean = false,
    val selectedModel: ModelOption = ModelOption.DEFAULT,
    val incentive: IncentiveConfig = IncentiveConfig(),
    val savedTick: Int = 0          // 自增以触发"已保存"提示
)

/**
 * 设置页 VM：API Key + 模型下拉 + 激励数值（§8 本地可调）。
 * Key 只进 SecureKeyStore，UI 不回显已存明文；激励数值进 IncentiveSettingsStore。
 */
class SettingsViewModel(
    private val keyStore: SecureKeyStore,
    private val incentiveStore: IncentiveSettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            hasSavedKey = keyStore.hasApiKey(),
            selectedModel = keyStore.getModelOption(),
            incentive = incentiveStore.getConfig()
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

    fun onIncentiveChange(config: IncentiveConfig) {
        _state.update { it.copy(incentive = config) }
    }

    fun save() {
        val input = _state.value.apiKeyInput
        if (input.isNotBlank()) {
            keyStore.setApiKey(input)
        }
        incentiveStore.setConfig(_state.value.incentive)
        // 回读一次，确保 UI 与落库后的规整值（如下限钳制）一致。
        val persisted = incentiveStore.getConfig()
        _state.update {
            it.copy(
                apiKeyInput = "",               // 存完即清空输入框，不在内存/UI 久留
                hasSavedKey = keyStore.hasApiKey(),
                incentive = persisted,
                savedTick = it.savedTick + 1
            )
        }
    }
}
