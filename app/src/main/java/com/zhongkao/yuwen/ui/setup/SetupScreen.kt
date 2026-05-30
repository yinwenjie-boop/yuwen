package com.zhongkao.yuwen.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.domain.usecase.ExerciseType
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import com.zhongkao.yuwen.domain.prompt.DifficultyProfile
import com.zhongkao.yuwen.domain.usecase.SettlementCalculator
import com.zhongkao.yuwen.ui.common.LabeledDropdown

/**
 * 出题设置页（阶段 2 + 阶段 4）：选大类/难度/题量/总分（现代文另选文体）。
 * 阶段 4：点"开始出题"先弹"练前目标卡"（§8 激励一），确认后进入答题；
 * 支持从复习页带入"侧重考点"（针对薄弱点再出一套）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    container: AppContainer,
    onStart: (GenerationRequest) -> Unit,
    onBack: () -> Unit
) {
    // 复习页带入的侧重考点（只取一次后清空）。
    val initialFocus = remember { container.pendingFocus?.also { container.pendingFocus = null } }
    // 首页"文言文/现代文练习"入口带入的预选题型（只取一次后清空）。
    val initialType = remember { container.pendingType?.also { container.pendingType = null } }

    var type by remember { mutableStateOf(initialType ?: ExerciseType.WENYAN_COMPARE) }
    var difficulty by remember { mutableStateOf("中等") }
    var count by remember { mutableStateOf(4) }
    var totalScore by remember { mutableStateOf(if (initialType == ExerciseType.XIANDAI) "25" else "15") }
    var genre by remember { mutableStateOf("记叙文") }
    var focus by remember { mutableStateOf(initialFocus.orEmpty()) }

    // 待确认的目标卡（非 null 时弹窗）。
    var pendingGoal by remember { mutableStateOf<GenerationRequest?>(null) }

    fun buildRequest() = GenerationRequest(
        type = type,
        difficulty = difficulty,
        questionCount = count,
        totalScore = totalScore.toIntOrNull()?.coerceIn(1, 150) ?: 15,
        genre = if (type == ExerciseType.XIANDAI) genre else null,
        focus = focus.ifBlank { null },
        // 字数随难度档增长，叙事性文体（记叙/散文/小说）再上浮 100 字。
        targetWords = DifficultyProfile.of(difficulty).xiandaiWords +
            if (genre == "记叙文" || genre == "散文" || genre == "小说") 100 else 0
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出题设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!initialFocus.isNullOrBlank()) {
                Text("本次针对薄弱点：$initialFocus", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            }

            Text("题型")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            type = t
                            totalScore = if (t == ExerciseType.WENYAN_COMPARE) "15" else "25"
                        },
                        label = { Text(t.label) }
                    )
                }
            }

            LabeledDropdown(
                label = "难度",
                options = DifficultyProfile.LABELS,
                selected = difficulty,
                onSelected = { difficulty = it }
            )

            LabeledDropdown(
                label = "题量",
                options = listOf("3", "4", "5"),
                selected = count.toString(),
                onSelected = { count = it.toInt() }
            )

            OutlinedTextField(
                value = totalScore,
                onValueChange = { v -> totalScore = v.filter { it.isDigit() }.take(3) },
                label = { Text("总分") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (type == ExerciseType.XIANDAI) {
                LabeledDropdown(
                    label = "文体",
                    options = listOf("记叙文", "散文", "小说", "说明文", "议论文", "非连续性文本"),
                    selected = genre,
                    onSelected = { genre = it }
                )
            }

            OutlinedTextField(
                value = focus,
                onValueChange = { focus = it },
                label = { Text(if (type == ExerciseType.WENYAN_COMPARE) "侧重考点（可选）" else "主题倾向（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { pendingGoal = buildRequest() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始出题")
            }
        }
    }

    // 练前 · 目标卡（§8 激励一）：建立即时目标感。
    pendingGoal?.let { req ->
        GoalCardDialog(
            req = req,
            container = container,
            onChallenge = {
                pendingGoal = null
                onStart(req)
            },
            onDismiss = { pendingGoal = null }
        )
    }
}

@Composable
private fun GoalCardDialog(
    req: GenerationRequest,
    container: AppContainer,
    onChallenge: () -> Unit,
    onDismiss: () -> Unit
) {
    val examConfig = container.examConfig
    val key = SettlementCalculator.baselineKey(req.type.apiType, req.genre)
    val baseline = examConfig.baselineRange(key, req.totalScore)
    val accPct = (examConfig.accuracyThreshold * 100).toInt()
    val upperMin = baseline?.let { (it.last + 59) / 60 }  // 上限分钟（向上取整）

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本次目标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("题型：${req.type.label}${if (req.type == ExerciseType.XIANDAI) "（${req.genre}）" else ""}")
                Text("满分 ${req.totalScore} 分 · ${req.questionCount} 题 · 难度${req.difficulty}")
                if (upperMin != null) {
                    Text("🎯 目标：正确率 ≥ $accPct% 且 用时 ≤ $upperMin 分钟", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                } else {
                    Text("🎯 目标：正确率 ≥ $accPct%（本题型暂无时间基准）", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                }
                Text("达标可获额外 XP 奖励，加油！")
            }
        },
        confirmButton = { Button(onClick = onChallenge) { Text("开始挑战") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("再改改") } }
    )
}
