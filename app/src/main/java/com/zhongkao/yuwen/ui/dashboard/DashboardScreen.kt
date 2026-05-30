package com.zhongkao.yuwen.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.data.repository.TextBankRepository

/**
 * 首页（阶段 1）：展示语料库课内/课外篇数，便于验证灌库与选篇基础。
 * 今日练习入口、连续打卡/等级、错题数、薄弱点 Top3 将在后续阶段填入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    textBankRepository: TextBankRepository,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardViewModel(textBankRepository) } }
    )
) {
    val kewen by viewModel.kewenCount.collectAsStateWithLifecycle()
    val kewai by viewModel.kewaiCount.collectAsStateWithLifecycle()

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "本地语料库",
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CountCard("课内固定语料", kewen, "篇", Modifier.weight(1f))
                CountCard("课外对比语料", kewai, "篇", Modifier.weight(1f))
            }
            Text(
                text = "课内 22 篇为不可由 AI 改写的固定原文；课外篇均带真实出处。\n出题与批改闭环将在下一阶段接入。",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp)
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
