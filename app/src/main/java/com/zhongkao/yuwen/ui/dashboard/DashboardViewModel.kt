package com.zhongkao.yuwen.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.data.repository.TextBankRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class CorpusCounts(val kewen: Int = 0, val kewai: Int = 0)

/** 首页 VM（阶段 1）：暴露语料库课内/课外篇数，用于肉眼验证灌库与选篇基础。 */
class DashboardViewModel(textBankRepository: TextBankRepository) : ViewModel() {

    val kewenCount: StateFlow<Int> = textBankRepository.observeKewenCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val kewaiCount: StateFlow<Int> = textBankRepository.observeKewaiCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
