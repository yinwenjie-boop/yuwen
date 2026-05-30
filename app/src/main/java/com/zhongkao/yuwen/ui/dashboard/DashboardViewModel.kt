package com.zhongkao.yuwen.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.db.UserProgress
import com.zhongkao.yuwen.data.db.WeakPointStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 首页 VM（阶段 1 + 阶段 4）：语料库篇数 + 进度（连续打卡/等级称号）+ 错题数 + 薄弱点 Top3。
 */
class DashboardViewModel(container: AppContainer) : ViewModel() {

    private val textBank = container.textBankRepository
    private val wrongBook = container.wrongBookRepository
    private val progressRepo = container.progressRepository

    val kewenCount: StateFlow<Int> = textBank.observeKewenCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val kewaiCount: StateFlow<Int> = textBank.observeKewaiCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 待人工确认的 AI 提议课外篇数（>0 时首页露出入口）。 */
    val pendingReviewCount: StateFlow<Int> = textBank.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val progress: StateFlow<UserProgress?> = progressRepo.observeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val wrongCount: StateFlow<Int> = wrongBook.observeUnmasteredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val topWeakPoints: StateFlow<List<WeakPointStat>> = wrongBook.observeTopWeakPoints(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
