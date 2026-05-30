package com.zhongkao.yuwen.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.db.TextBank
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 课外篇人工确认页 VM（阶段 5）。
 * 列出 AI 提议的、verified=false 的课外篇；确认后置 verified=true 方可入正式题库，
 * 拒绝则删除。这是「AI 绝不自动把未核实课外篇入库」硬约束的人工闸门。
 */
class PendingReviewViewModel(private val container: AppContainer) : ViewModel() {

    val pending: StateFlow<List<TextBank>> =
        container.textBankRepository.observePendingReview()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 确认真实性 → 入正式题库。 */
    fun confirm(id: Long) {
        viewModelScope.launch { container.textBankRepository.confirmPending(id) }
    }

    /** 出处存疑/不合规 → 删除，绝不入库。 */
    fun reject(id: Long) {
        viewModelScope.launch { container.textBankRepository.rejectPending(id) }
    }
}
