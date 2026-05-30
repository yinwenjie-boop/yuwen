package com.zhongkao.yuwen.ui.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.db.ExerciseTimePoint
import com.zhongkao.yuwen.data.db.WeakPointStat

/**
 * 复习页（阶段 4）：用时曲线 + 薄弱点（可针对再练）+ 错题本按考点归类。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onRePractice: () -> Unit,
    viewModel: ReviewViewModel = viewModel(
        factory = viewModelFactory { initializer { ReviewViewModel(container) } }
    )
) {
    val timePoints by viewModel.timePoints.collectAsStateWithLifecycle()
    val weakPoints by viewModel.weakPoints.collectAsStateWithLifecycle()
    val wrongGroups by viewModel.wrongGroups.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("错题与薄弱点") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("近 ${timePoints.size} 次用时曲线") }
            item { TimeCurveCard(timePoints) }

            item { SectionTitle("薄弱点（按错题数排序）") }
            if (weakPoints.isEmpty()) {
                item { Text("暂无薄弱点统计，先去练几套吧。", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(weakPoints) { wp ->
                    WeakPointCard(wp) {
                        viewModel.prepareRePractice(wp.abilityTag)
                        onRePractice()
                    }
                }
            }

            item { SectionTitle("错题本（按考点归类）") }
            if (wrongGroups.isEmpty()) {
                item { Text("错题本是空的，继续保持！", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(wrongGroups) { group ->
                    WrongGroupCard(group, onMastered = viewModel::markMastered)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun TimeCurveCard(points: List<ExerciseTimePoint>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (points.size < 2) {
                Text("用时曲线需至少完成 2 次练习。", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val lineColor = MaterialTheme.colorScheme.primary
            val maxSec = (points.maxOf { it.totalTimeSec }).coerceAtLeast(1)
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val w = size.width
                val h = size.height
                val stepX = if (points.size > 1) w / (points.size - 1) else w
                val offsets = points.mapIndexed { i, p ->
                    val x = stepX * i
                    val y = h - (p.totalTimeSec.toFloat() / maxSec) * h
                    Offset(x, y)
                }
                for (i in 0 until offsets.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = offsets[i],
                        end = offsets[i + 1],
                        strokeWidth = 5f
                    )
                }
                offsets.forEach { drawCircle(color = lineColor, radius = 7f, center = it) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("旧", style = MaterialTheme.typography.labelSmall)
                Text("每点=一次练习总用时（峰值 ${maxSec / 60} 分 ${maxSec % 60} 秒）", style = MaterialTheme.typography.labelSmall)
                Text("新", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun WeakPointCard(wp: WeakPointStat, onRePractice: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(wp.abilityTag.ifBlank { "未分类" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            val rate = if (wp.attempts > 0) wp.wrongCount * 100 / wp.attempts else 0
            Text("作答 ${wp.attempts} 次 · 错 ${wp.wrongCount} 次（错误率 $rate%）· 平均用时 ${wp.avgTimeSec} 秒")
            OutlinedButton(onClick = onRePractice, modifier = Modifier.fillMaxWidth()) {
                Text("针对此点再练一套")
            }
        }
    }
}

@Composable
private fun WrongGroupCard(group: WrongGroup, onMastered: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${group.abilityTag}（${group.items.size}）", fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "收起" else "展开")
                }
            }
            if (expanded) {
                group.items.forEach { item ->
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("· ${item.qType}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(item.stem, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onMastered(item.wrongId) }) { Text("标记已掌握") }
                    }
                }
            }
        }
    }
}
