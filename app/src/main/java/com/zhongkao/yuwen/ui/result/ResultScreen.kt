package com.zhongkao.yuwen.ui.result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    container: AppContainer,
    exerciseId: Long,
    onExit: () -> Unit,
    viewModel: ResultViewModel = viewModel(
        factory = viewModelFactory { initializer { ResultViewModel(container, exerciseId) } }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("批改结果") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ResultUiState.Loading -> Center {
                    CircularProgressIndicator()
                    Text("AI 批改中，请稍候…", modifier = Modifier.padding(top = 16.dp))
                }
                is ResultUiState.Failed -> Center {
                    Text(s.message, style = MaterialTheme.typography.bodyLarge)
                    Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onExit) { Text("返回") }
                        Button(onClick = viewModel::load) { Text("重试") }
                    }
                }
                is ResultUiState.Content -> ResultContent(s.view, onExit)
            }
        }
    }
}

@Composable
private fun ResultContent(view: ResultView, onExit: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SettlementCard(view.reward) }
        item { SummaryCard(view) }
        items(view.items) { item -> QuestionCard(item) }
        item {
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("返回首页") }
        }
    }
}

/** 练后即时结算（激励四件套之一）：XP 进度条增长 + 积分跳动 + 等级/连胜/徽章。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettlementCard(reward: RewardView) {
    // 进入页面后触发动画：进度条从 0 涨到当前等级内进度，积分从 0 跳到本次所得。
    var play by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { play = true }

    val targetFraction = if (reward.xpPerLevel > 0)
        (reward.xpIntoLevel.toFloat() / reward.xpPerLevel).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = if (play) targetFraction else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "xpBar"
    )
    val animatedCoins by animateIntAsState(
        targetValue = if (play) reward.coinsGained else 0,
        animationSpec = tween(durationMillis = 900),
        label = "coins"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lv.${reward.level} ${reward.levelTitle}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("🔥 连续 ${reward.streakDays} 天", style = MaterialTheme.typography.titleSmall)
            }
            if (reward.leveledUp) {
                Text("🎉 升级啦！解锁称号「${reward.levelTitle}」", fontWeight = FontWeight.Bold)
            }

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier.fillMaxWidth()
            )
            Text("XP ${reward.xpIntoLevel} / ${reward.xpPerLevel}（累计 ${reward.totalXp}）", style = MaterialTheme.typography.labelMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (reward.freshlyGraded) {
                    Text("本次 +${reward.xpGained} XP", fontWeight = FontWeight.Bold)
                    Text("💰 +$animatedCoins 积分", fontWeight = FontWeight.Bold)
                } else {
                    Text("（本次成绩已计入，重看不重复发放）", style = MaterialTheme.typography.labelMedium)
                }
            }
            Text("积分余额 ${reward.totalCoins}", style = MaterialTheme.typography.labelMedium)

            if (reward.newBadges.isNotEmpty()) {
                Text("获得徽章", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reward.newBadges.forEach { name ->
                        AssistChip(onClick = {}, label = { Text("🏅 $name") })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(view: ResultView) {
    val s = view.settlement
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("总分 ${view.totalGot} / ${view.totalFull}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("正确率 ${(s.accuracy * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            val mm = s.timeSpentSec / 60
            val ss = s.timeSpentSec % 60
            val low = s.baselineRange.getOrElse(0) { 0 } / 60
            val high = s.baselineRange.getOrElse(1) { 0 } / 60
            Text("用时 ${mm}分${ss}秒　基准约 ${low}–${high} 分钟")
            Text("时间评价：${s.timeRating}", fontWeight = FontWeight.Bold)
            s.slowestQuestion?.let { Text("最耗时：${it.id}（${it.sec} 秒）") }
            if (view.weakPoints.isNotEmpty()) {
                Text("薄弱点：${view.weakPoints.joinToString("、")}")
            }
            if (view.nextAdvice.isNotBlank()) {
                Text("下一步：${view.nextAdvice}")
            }
        }
    }
}

@Composable
private fun QuestionCard(item: GradedItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${item.qType}", fontWeight = FontWeight.Bold)
                Text("${item.gotScore} / ${item.maxScore} 分", fontWeight = FontWeight.Bold)
            }
            Text(item.stem, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()

            Label("你的作答")
            Text(item.studentAnswer.ifBlank { "（未作答）" })

            if (item.pointChecks.isNotEmpty()) {
                Label("采分点")
                item.pointChecks.forEach { pc ->
                    Text("${if (pc.hit) "✓" else "✗"} ${pc.point}（${pc.score} 分）")
                }
            }
            if (item.errorType.isNotBlank()) { Label("错因"); Text(item.errorType) }
            if (item.correctAnswer.isNotBlank()) { Label("正确答法"); Text(item.correctAnswer) }
            if (item.explanation.isNotBlank()) { Label("解析"); Text(item.explanation) }
            if (item.tip.isNotBlank()) { Label("提升建议"); Text(item.tip) }
            Text("本题用时 ${item.timeSec} 秒", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Center(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}
