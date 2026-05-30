package com.zhongkao.yuwen.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.zhongkao.yuwen.domain.usecase.ExerciseType
import com.zhongkao.yuwen.domain.usecase.GenerationRequest
import com.zhongkao.yuwen.ui.common.LabeledDropdown

/**
 * 出题设置页（阶段 2）：选大类/难度/题量/总分（现代文另选文体）。
 * §8 的"目标卡"激励在阶段 4 接入，这里先不做。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onStart: (GenerationRequest) -> Unit,
    onBack: () -> Unit
) {
    var type by remember { mutableStateOf(ExerciseType.WENYAN_COMPARE) }
    var difficulty by remember { mutableStateOf("中等") }
    var count by remember { mutableStateOf(4) }
    var totalScore by remember { mutableStateOf("15") }
    var genre by remember { mutableStateOf("记叙文") }
    var focus by remember { mutableStateOf("") }

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
                options = listOf("基础", "中等", "偏难"),
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
                    options = listOf("记叙文", "散文", "说明文", "议论文"),
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
                onClick = {
                    onStart(
                        GenerationRequest(
                            type = type,
                            difficulty = difficulty,
                            questionCount = count,
                            totalScore = totalScore.toIntOrNull()?.coerceIn(1, 150) ?: 15,
                            genre = if (type == ExerciseType.XIANDAI) genre else null,
                            focus = focus.ifBlank { null },
                            targetWords = if (genre == "记叙文" || genre == "散文") 900 else 800
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始出题")
            }
        }
    }
}
