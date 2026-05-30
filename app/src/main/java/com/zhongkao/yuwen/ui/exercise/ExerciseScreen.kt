package com.zhongkao.yuwen.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.ai.GeneratedExercise
import com.zhongkao.yuwen.data.ai.Passage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    container: AppContainer,
    onExit: () -> Unit,
    viewModel: ExerciseViewModel = viewModel(
        factory = viewModelFactory { initializer { ExerciseViewModel(container) } }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 切后台暂停计时、回前台恢复（SPEC §7.2）。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.pauseTimer()
                Lifecycle.Event.ON_START -> viewModel.resumeTimer()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("答题") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ExerciseUiState.Loading -> CenterMessage("AI 出题中，请稍候…", showSpinner = true)
                is ExerciseUiState.Failed -> ErrorBlock(s.message, onRetry = viewModel::start, onExit = onExit)
                is ExerciseUiState.Invalid -> ErrorBlock(
                    "出题校验未通过：\n" + s.errors.joinToString("\n") { "· $it" },
                    onRetry = viewModel::start, onExit = onExit
                )
                is ExerciseUiState.NeedsReview -> ErrorBlock(
                    "本次出现需人工确认的课外篇，已拦截不入库：\n" + s.warnings.joinToString("\n") { "· $it" },
                    onRetry = viewModel::start, onExit = onExit
                )
                is ExerciseUiState.Submitted -> SubmittedBlock(s.totalSec, onExit)
                is ExerciseUiState.Answering -> AnsweringBlock(s.generated, viewModel)
            }
        }
    }
}

@Composable
private fun AnsweringBlock(generated: GeneratedExercise, viewModel: ExerciseViewModel) {
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val questions = generated.questions
    if (questions.isEmpty()) {
        CenterMessage("本次没有题目，请返回重试。")
        return
    }
    val q = questions[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PassagePanel(generated)

        Text(
            "第 ${index + 1} / ${questions.size} 题 · ${q.qType} · ${q.maxScore} 分",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(q.stem, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = answers[q.id].orEmpty(),
            onValueChange = { viewModel.onAnswerChange(q.id, it) },
            label = { Text("你的作答") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.goTo(index - 1) },
                enabled = index > 0,
                modifier = Modifier.weight(1f)
            ) { Text("上一题") }

            if (index < questions.size - 1) {
                Button(
                    onClick = { viewModel.goTo(index + 1) },
                    modifier = Modifier.weight(1f)
                ) { Text("下一题") }
            } else {
                Button(
                    onClick = { viewModel.submit() },
                    modifier = Modifier.weight(1f)
                ) { Text("提交") }
            }
        }
    }
}

@Composable
private fun PassagePanel(generated: GeneratedExercise) {
    var expanded by remember { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("原文（可回看）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "收起" else "展开")
                }
            }
            if (expanded) {
                generated.passageIn?.let { PassageText("【甲·课内】", it) }
                generated.passageOut?.let { PassageText("【乙·课外】", it) }
                generated.passage?.let { PassageText("【原文】", it) }
            }
        }
    }
}

@Composable
private fun PassageText(tag: String, p: Passage) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        val head = buildString {
            append(tag)
            if (p.title.isNotBlank()) append(p.title)
            if (p.author.isNotBlank()) append("（${p.author}）")
        }
        Text(head, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(p.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        if (p.sourceRef.isNotBlank()) {
            Text("出处：${p.sourceRef}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun SubmittedBlock(totalSec: Int, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("已提交", style = MaterialTheme.typography.headlineSmall)
        Text(
            "本次用时 ${totalSec / 60} 分 ${totalSec % 60} 秒，每题用时已记录。\n踩点批改与时间评价将在下一阶段接入。",
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(onClick = onExit, modifier = Modifier.padding(top = 20.dp)) { Text("返回首页") }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onExit) { Text("返回") }
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun CenterMessage(message: String, showSpinner: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showSpinner) {
            CircularProgressIndicator()
            Text(message, modifier = Modifier.padding(top = 16.dp))
        } else {
            Text(message)
        }
    }
}
