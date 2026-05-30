package com.zhongkao.yuwen.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.AppContainer

/**
 * 首页（阶段 1 + 阶段 4）：进度卡（连续打卡/等级称号/错题数）+ 薄弱点 Top3 + 语料库篇数。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    container: AppContainer,
    onStartPractice: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardViewModel(container) } }
    )
) {
    val kewen by viewModel.kewenCount.collectAsStateWithLifecycle()
    val kewai by viewModel.kewaiCount.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val wrongCount by viewModel.wrongCount.collectAsStateWithLifecycle()
    val weakPoints by viewModel.topWeakPoints.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("中考语文备考") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 进度卡（激励四件套之"连续打卡 + 等级"）。
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Lv.${progress?.level ?: 1} ${progress?.levelTitle ?: "文言学徒"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("🔥 连续 ${progress?.streakDays ?: 0} 天", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("XP ${progress?.xp ?: 0}")
                        Text("💰 ${progress?.coins ?: 0}")
                        Text("错题 $wrongCount")
                    }
                }
            }

            // 薄弱点 Top3。
            if (weakPoints.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("薄弱点 Top3", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        weakPoints.forEach { wp ->
                            Text("· ${wp.abilityTag.ifBlank { "未分类" }}：错 ${wp.wrongCount} / 答 ${wp.attempts}，平均 ${wp.avgTimeSec} 秒")
                        }
                    }
                }
            }

            Button(onClick = onStartPractice, modifier = Modifier.fillMaxWidth()) {
                Text("开始练习")
            }
            OutlinedButton(onClick = onOpenReview, modifier = Modifier.fillMaxWidth()) {
                Text("错题与薄弱点（$wrongCount 道错题）")
            }

            // 语料库篇数（阶段 1 验证）。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CountCard("课内固定语料", kewen, "篇", Modifier.weight(1f))
                CountCard("课外对比语料", kewai, "篇", Modifier.weight(1f))
            }
            Text(
                text = "课内为不可由 AI 改写的固定原文；课外篇均带真实出处。",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CountCard(label: String, count: Int, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", style = MaterialTheme.typography.headlineMedium)
            Text(text = unit, style = MaterialTheme.typography.labelSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
