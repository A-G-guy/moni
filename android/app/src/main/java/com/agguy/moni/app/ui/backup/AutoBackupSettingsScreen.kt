@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.backup

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agguy.moni.app.components.SettingsItem
import com.agguy.moni.app.components.SettingsToggleItem
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.core.platform.AutoBackupScheduler
import com.agguy.moni.core.platform.DataStoreHelper
import kotlinx.coroutines.launch

/**
 * 自动备份设置页面。
 *
 * 内部直接通过 [DataStoreHelper] 读写配置，不依赖外部状态传递。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var showFrequencyDialog by remember { mutableStateOf(false) }

    val enabled by DataStoreHelper.autoBackupEnabledFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val frequency by DataStoreHelper.autoBackupFrequencyFlow(context)
        .collectAsStateWithLifecycle(initialValue = "daily")
    val maxCount by DataStoreHelper.autoBackupMaxCountFlow(context)
        .collectAsStateWithLifecycle(initialValue = 7)
    // 滑块本地状态，避免滑动过程中频繁写入 DataStore
    var sliderValue by remember { mutableFloatStateOf(maxCount.toFloat()) }
    // 当 DataStore 值变化时同步本地状态（如从其他页面返回）
    androidx.compose.runtime.LaunchedEffect(maxCount) {
        sliderValue = maxCount.toFloat()
    }
    val copyToExternal by DataStoreHelper.autoBackupCopyToExternalFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val externalUri by DataStoreHelper.autoBackupExternalUriFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)
    val lastBackupTime by DataStoreHelper.autoBackupLastTimeFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)

    // 配置变化时重新调度 WorkManager
    LaunchedEffect(enabled, frequency) {
        AutoBackupScheduler.schedule(context, enabled, frequency)
    }

    val frequencyLabel = when (frequency) {
        "every_launch" -> "每次启动"
        "daily" -> "每天"
        "weekly" -> "每周"
        "monthly" -> "每月"
        else -> "每天"
    }

    val externalDirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            scope.launch {
                DataStoreHelper.saveAutoBackupExternalUri(context, it.toString())
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("自动备份") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("完成")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsToggleItem(
                iconName = "cloud",
                title = "启用自动备份",
                subtitle = if (enabled) "已启用" else "已关闭",
                checked = enabled,
                onCheckedChange = {
                    scope.launch { DataStoreHelper.saveAutoBackupEnabled(context, it) }
                }
            )

            AnimatedVisibility(visible = enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsItem(
                        iconName = "event",
                        title = "备份频率",
                        subtitle = frequencyLabel,
                        onClick = { showFrequencyDialog = true }
                    )

                    // 保留数量滑块
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "保留数量",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "${sliderValue.toInt()} 个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    DataStoreHelper.saveAutoBackupMaxCount(context, sliderValue.toInt())
                                }
                            },
                            valueRange = 3f..30f,
                            steps = 26
                        )
                        Text(
                            "超出保留数量的旧备份将自动删除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    SettingsToggleItem(
                        iconName = "archive",
                        title = "复制到外部目录",
                        subtitle = if (copyToExternal) "已启用" else "关闭",
                        checked = copyToExternal,
                        onCheckedChange = {
                            scope.launch {
                                DataStoreHelper.saveAutoBackupCopyToExternal(context, it)
                                if (!it) {
                                    DataStoreHelper.saveAutoBackupExternalUri(context, null)
                                }
                            }
                        }
                    )

                    AnimatedVisibility(visible = copyToExternal) {
                        SettingsItem(
                            iconName = "archive",
                            title = "选择外部目录",
                            subtitle = externalUri ?: "未选择",
                            onClick = { externalDirPickerLauncher.launch(null) }
                        )
                    }

                    if (lastBackupTime != null) {
                        Text(
                            "上次备份: $lastBackupTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showFrequencyDialog) {
        FrequencyPickerDialog(
            currentFrequency = frequency,
            onConfirm = { selected ->
                scope.launch {
                    DataStoreHelper.saveAutoBackupFrequency(context, selected)
                }
                showFrequencyDialog = false
            },
            onDismiss = { showFrequencyDialog = false }
        )
    }
}

@Composable
private fun FrequencyPickerDialog(
    currentFrequency: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        "every_launch" to "每次启动",
        "daily" to "每天",
        "weekly" to "每周",
        "monthly" to "每月"
    )
    var selected by remember { mutableStateOf(currentFrequency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("选择备份频率") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = { selected = value }
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
