package com.zhongkao.yuwen.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.db.ExerciseTimePoint
import com.zhongkao.yuwen.data.db.WeakPointStat
import com.zhongkao.yuwen.data.db.WrongQuestionDetail
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 错题按考点归类的一组。 */
data class WrongGroup(
    val abilityTag: String,
    val items: List<WrongQuestionDetail>
)

/**
 * 复习页 VM（阶段 4）：错题本按考点归类、薄弱点统计、近 N 次用时曲线。
 * "针对薄弱点再出一套"通过 [AppContainer.pendingFocus] 把考点带到出题设置页。
 */
class ReviewViewModel(private val container: AppContainer) : ViewModel() {

    /** 用时曲线：取近 N 次已批改练习，按时间升序（旧→新）便于画图。 */
    val timePoints: StateFlow<List<ExerciseTimePoint>> =
        container.exerciseRepository.observeRecentGraded(RECENT_N)
            .map { it.asReversed() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weakPoints: StateFlow<List<WeakPointStat>> =
        container.wrongBookRepository.observeAllWeakPoints()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wrongGroups: StateFlow<List<WrongGroup>> =
        container.wrongBookRepository.observeWrongDetails()
            .map { details ->
                details.groupBy { it.abilityTag.ifBlank { "未分类" } }
                    .map { (tag, items) -> WrongGroup(tag, items) }
                    .sortedByDescending { it.items.size }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markMastered(wrongId: Long) {
        viewModelScope.launch { container.wrongBookRepository.markMastered(wrongId) }
    }

    /** 暂存薄弱考点，供出题设置页预填侧重。 */
    fun prepareRePractice(abilityTag: String) {
        container.pendingFocus = abilityTag
    }

    companion object {
        const val RECENT_N = 10
    }
}
