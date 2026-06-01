package com.zhongkao.yuwen.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.core.json.AppJson
import com.zhongkao.yuwen.data.ai.GradingResult
import com.zhongkao.yuwen.data.db.ExerciseSummary
import com.zhongkao.yuwen.domain.usecase.ExerciseType
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 历史列表的一行（已从快照解析出难度/文体/得分/日期）。 */
data class HistoryItem(
    val id: Long,
    val typeLabel: String,
    val difficulty: String,
    val gotScore: Int,
    val fullScore: Int,
    val totalTimeSec: Int,
    val dateLabel: String
)

/**
 * 练习历史页 VM（只读回看 + 可删除）。
 * 列表只读"已批改"练习；点条目复用结果页从快照重渲染；删除走级联清理。
 */
class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    val items: StateFlow<List<HistoryItem>> =
        container.exerciseRepository.observeGradedSummaries()
            .map { list -> list.map { it.toItem() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { container.exerciseRepository.deleteExercise(id) }
    }

    private fun ExerciseSummary.toItem(): HistoryItem {
        val req = runCatching {
            AppJson.decodeFromString(GenerationRequest.serializer(), configJson)
        }.getOrNull()
        val grading = runCatching {
            AppJson.decodeFromString(GradingResult.serializer(), gradingJson)
        }.getOrNull()

        val exType = ExerciseType.fromApiType(type)
        val label = when {
            exType == ExerciseType.XIANDAI && !req?.genre.isNullOrBlank() -> "现代文阅读（${req?.genre}）"
            exType != null -> exType.label
            else -> type
        }
        return HistoryItem(
            id = id,
            typeLabel = label,
            difficulty = req?.difficulty ?: "—",
            gotScore = grading?.totalGot ?: 0,
            fullScore = grading?.totalFull ?: 0,
            totalTimeSec = totalTimeSec,
            dateLabel = DATE_FMT.format(Date(createdAt))
        )
    }

    private companion object {
        // 单用户本地 App：用设备默认时区/区域格式化练习时间即可。
        val DATE_FMT = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    }
}
