package com.zhongkao.yuwen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.data.backup.BackupManager
import com.zhongkao.yuwen.data.seed.CalibrationStore
import com.zhongkao.yuwen.domain.ExamConfigMeta
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.secureKeyStore, container.incentiveSettingsStore) }
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var savedHint by remember { mutableStateOf("") }

    LaunchedEffect(state.savedTick) {
        if (state.savedTick > 0) savedHint = "已安全保存"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("DeepSeek API Key")
            Text(
                if (state.hasSavedKey) "已保存一个 Key（不回显）。重新输入可覆盖。" else "尚未设置 Key。",
            )
            OutlinedTextField(
                value = state.apiKeyInput,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("输入 API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("模型")
            ModelDropdown(
                selected = state.selectedModel.display,
                options = viewModel.modelOptions.map { it.display },
                onSelectedIndex = { idx -> viewModel.onModelSelected(viewModel.modelOptions[idx]) }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Text("激励数值（本地计算，可调）", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            val inc = state.incentive
            IntField("每得 1 分 → XP", inc.xpPerScore) { viewModel.onIncentiveChange(inc.copy(xpPerScore = it)) }
            IntField("达标额外 XP", inc.targetBonusXp) { viewModel.onIncentiveChange(inc.copy(targetBonusXp = it)) }
            IntField("每得 1 分 → 积分", inc.coinsPerScore) { viewModel.onIncentiveChange(inc.copy(coinsPerScore = it)) }
            IntField("每日打卡积分", inc.streakDailyCoins) { viewModel.onIncentiveChange(inc.copy(streakDailyCoins = it)) }
            IntField("每级所需 XP", inc.xpPerLevel) { viewModel.onIncentiveChange(inc.copy(xpPerLevel = it)) }

            Spacer(Modifier.height(8.dp))
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("保存")
            }
            if (savedHint.isNotEmpty()) Text(savedHint)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            CalibrationSection(
                calibrationStore = container.calibrationStore,
                meta = container.examConfig.meta
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            BackupSection(backupManager = container.backupManager)
        }
    }
}

/**
 * 数据备份/恢复（阶段 6）：把错题本、薄弱点、进度、徽章导出为本地 JSON，可导入恢复。
 * 绝不导出 API Key。建议在干净安装上导入恢复（重复导入会叠加错题）。
 */
@Composable
private fun BackupSection(backupManager: BackupManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hint by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = backupManager.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    ?: error("无法写入所选位置")
            }.onSuccess {
                hint = "已导出备份（不含 API Key）。请妥善保存该 JSON 文件。"
            }.onFailure {
                hint = "导出失败：${it.message ?: "未知错误"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("无法读取所选文件")
                backupManager.importFromJson(json)
            }.onSuccess { s ->
                hint = "已恢复：错题 ${s.wrongQuestions} 道、薄弱点 ${s.weakPoints} 个、" +
                    "徽章 ${s.badges} 枚${if (s.progressRestored) "、进度已恢复" else ""}。"
            }.onFailure {
                hint = "导入失败：${it.message ?: "文件不是有效的备份"}（未改动现有数据）"
            }
        }
    }

    Text("数据备份与恢复", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
    Text(
        "导出错题本、薄弱点、进度与徽章为本地 JSON（不含 API Key）；换机或重装后可导入恢复。" +
            "建议在干净安装上导入，重复导入会叠加错题。",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
    )
    Button(
        onClick = { exportLauncher.launch("yuwen-backup.json") },
        modifier = Modifier.fillMaxWidth()
    ) { Text("导出备份 JSON") }
    OutlinedButton(
        onClick = { importLauncher.launch("application/json") },
        modifier = Modifier.fillMaxWidth()
    ) { Text("从备份 JSON 恢复") }
    if (hint.isNotEmpty()) {
        Text(hint, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 命题校准入口（阶段 5）：展示当前 exam_config 校准状态与校准指引，
 * 并允许导入按官方命题说明改好的 exam_config.json（覆盖内置估计值，重启生效）。
 */
@Composable
private fun CalibrationSection(
    calibrationStore: CalibrationStore,
    meta: ExamConfigMeta
) {
    val context = LocalContext.current
    var hasOverride by remember { mutableStateOf(calibrationStore.hasOverride()) }
    var hint by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取所选文件")
            calibrationStore.saveOverride(json)
        }.onSuccess {
            hasOverride = true
            hint = "校准文件已导入并通过解析校验，重启应用后生效。"
        }.onFailure {
            hint = "导入失败：${it.message ?: "文件不是合法的 exam_config.json"}（未改动现有配置）"
        }
    }

    Text("命题校准（exam_config）", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
    val statusText = when {
        hasOverride -> "已载入校准覆盖文件（优先于内置估计值）"
        meta.isCalibrated -> "已用官方命题说明校准"
        else -> "当前为估计值（真题反推 + 公开资料），尚未用官方命题说明校准"
    }
    Text("状态：$statusText", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    if (meta.region.isNotBlank() || meta.examYear > 0) {
        Text("适用：${meta.region}${if (meta.examYear > 0) " ${meta.examYear} 年" else ""}",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
    if (meta.calibrationInstruction.isNotEmpty()) {
        Text("校准步骤：", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        meta.calibrationInstruction.forEach { line ->
            Text("· $line", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }

    Button(
        onClick = { importLauncher.launch("application/json") },
        modifier = Modifier.fillMaxWidth()
    ) { Text("导入校准后的 exam_config.json") }

    if (hasOverride) {
        OutlinedButton(
            onClick = {
                calibrationStore.clearOverride()
                hasOverride = false
                hint = "已恢复内置估计值，重启应用后生效。"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("恢复内置估计值") }
    }

    if (hint.isNotEmpty()) {
        Text(hint, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IntField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    selected: String,
    options: List<String>,
    onSelectedIndex: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("DeepSeek 模型") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelectedIndex(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
