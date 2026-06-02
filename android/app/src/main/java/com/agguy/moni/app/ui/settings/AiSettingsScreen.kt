@file:OptIn(ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.model.AiApiFormat
import com.agguy.moni.app.model.AiProviderPreset
import com.agguy.moni.app.model.AiThinkingLevel

/** AI Provider 设置页。 */
@Composable
fun AiSettingsScreen(
    viewModel: AiSettingsViewModel,
    aiBookkeepingEnabled: Boolean,
    aiChatRetentionDays: Int,
    onAiBookkeepingEnabledChange: (Boolean) -> Unit,
    onAiChatRetentionDaysChange: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("AI 设置", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(name = "arrow_back", contentDescription = "返回", size = 24.dp)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::startCreate) {
                        SymbolIcon(name = "add", contentDescription = "新增", size = 24.dp)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "管理可自定义的 AI Provider 预设。请求和解析均由 Rust Core 处理；API Key 仅在保存和请求时使用，列表中会脱敏显示。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AiBookkeepingSwitchCard(
                enabled = aiBookkeepingEnabled,
                onEnabledChange = onAiBookkeepingEnabledChange,
            )
            AiChatRetentionCard(
                selectedDays = aiChatRetentionDays,
                onSelectedDaysChange = onAiChatRetentionDaysChange,
            )
            uiState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (uiState.presets.isEmpty() && !uiState.isLoading) {
                Text(
                    text = "暂无 AI 预设，点击右上角 + 新增。",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            uiState.presets.forEach { preset ->
                AiPresetCard(
                    preset = preset,
                    onEdit = { viewModel.startEdit(preset) },
                    onDelete = { viewModel.deletePreset(preset.id) },
                    onSetDefault = { viewModel.setDefault(preset.id) },
                    onTest = { viewModel.testConnection(preset.id) },
                )
            }
        }
    }

    uiState.editingPreset?.let { form ->
        AiPresetEditorDialog(
            form = form,
            onFormChange = viewModel::updateForm,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEditing,
        )
    }
}

@Composable
private fun AiBookkeepingSwitchCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    MoniCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("启用 AI 记账", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "关闭后隐藏账单页右下角 AI 入口，不影响 Provider 预设配置的保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun AiChatRetentionCard(
    selectedDays: Int,
    onSelectedDaysChange: (Int) -> Unit,
) {
    val options = listOf(
        7 to "7 天",
        30 to "30 天",
        90 to "90 天",
        365 to "1 年",
        0 to "永久",
    )
    MoniCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("聊天记录保留", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "进入 AI 记账时会自动清理超过保留时间的聊天记录。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (days, label) ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { onSelectedDaysChange(days) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPresetCard(
    preset: AiProviderPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onTest: () -> Unit,
) {
    MoniCard(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preset.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (preset.isDefault) {
                    Text("默认", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("格式：${preset.apiFormat.displayName}", style = MaterialTheme.typography.bodyMedium)
            Text("模型：${preset.model}", style = MaterialTheme.typography.bodyMedium)
            Text("Base URL：${preset.baseUrl}", style = MaterialTheme.typography.bodySmall)
            Text("Key：${preset.maskedApiKey.ifBlank { "未设置" }}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "思考：${preset.thinkingLevel.displayName} · 读图：${if (preset.supportsVision) "支持" else "不支持"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onTest) { Text("测试") }
                if (!preset.isDefault) {
                    TextButton(onClick = onSetDefault) { Text("设为默认") }
                }
                TextButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}

@Composable
private fun AiPresetEditorDialog(
    form: AiPresetFormState,
    onFormChange: (AiPresetFormState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id == null) "新增 AI 预设" else "编辑 AI 预设") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("API 格式", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiApiFormat.entries.forEach { format ->
                        FilterChip(
                            selected = form.apiFormat == format,
                            onClick = {
                                val defaults = defaultsFor(format)
                                onFormChange(
                                    form.copy(
                                        apiFormat = format,
                                        baseUrl = defaults.first,
                                        model = defaults.second,
                                    )
                                )
                            },
                            label = { Text(format.displayName) },
                        )
                    }
                }
                OutlinedTextField(
                    value = form.baseUrl,
                    onValueChange = { onFormChange(form.copy(baseUrl = it)) },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.apiKey,
                    onValueChange = { onFormChange(form.copy(apiKey = it)) },
                    label = { Text(if (form.id == null) "API Key" else "API Key（留空则保留原值）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.model,
                    onValueChange = { onFormChange(form.copy(model = it)) },
                    label = { Text("模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("思考程度", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiThinkingLevel.entries.forEach { level ->
                        FilterChip(
                            selected = form.thinkingLevel == level,
                            onClick = { onFormChange(form.copy(thinkingLevel = level)) },
                            label = { Text(level.displayName) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("模型支持读图", modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.supportsVision,
                        onCheckedChange = { onFormChange(form.copy(supportsVision = it)) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("设为默认预设", modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.isDefault,
                        onCheckedChange = { onFormChange(form.copy(isDefault = it)) },
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun defaultsFor(format: AiApiFormat): Pair<String, String> = when (format) {
    AiApiFormat.OpenAiChatCompletions -> "https://api.openai.com/v1" to "gpt-4o-mini"
    AiApiFormat.GeminiGenerateContent -> "https://generativelanguage.googleapis.com/v1beta" to "gemini-2.5-flash"
}
