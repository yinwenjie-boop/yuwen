package com.zhongkao.yuwen.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.db.TextBank

/**
 * 课外篇人工确认页（阶段 5）：列出 AI 提议、待确认的课外篇，逐条核对真实出处后
 * 「确认入库」(verified=true) 或「删除」。未确认者绝不会被出题选中。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingReviewScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: PendingReviewViewModel = viewModel(
        factory = viewModelFactory { initializer { PendingReviewViewModel(container) } }
    )
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待确认课外篇") },
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
            item {
                Text(
                    "以下课外篇由 AI 提议、出处待核实。请逐条核对真实出处后再决定：" +
                        "确认入库后才可被出题选中；存疑请删除。课内篇为固定原文，不在此列。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (pending.isEmpty()) {
                item {
                    Text("当前没有待确认的课外篇。", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(pending) { tb ->
                    PendingCard(
                        item = tb,
                        onConfirm = { viewModel.confirm(tb.id) },
                        onReject = { viewModel.reject(tb.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingCard(item: TextBank, onConfirm: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val head = buildString {
                append(item.title.ifBlank { "未命名课外篇" })
                if (item.author.isNotBlank()) append("（${item.author}")
                if (item.dynasty.isNotBlank()) append("·${item.dynasty}")
                if (item.author.isNotBlank() || item.dynasty.isNotBlank()) append("）")
            }
            Text(head, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                "出处：${item.sourceRef.ifBlank { "（未注明，需谨慎核实）" }}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                item.text.ifBlank { "（AI 未给出原文，建议删除）" },
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("删除") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("确认入库") }
            }
        }
    }
}
