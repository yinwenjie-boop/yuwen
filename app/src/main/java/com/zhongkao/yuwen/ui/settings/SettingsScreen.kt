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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.core.settings.IncentiveSettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    keyStore: SecureKeyStore,
    incentiveStore: IncentiveSettingsStore,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(keyStore, incentiveStore) } }
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
        }
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
