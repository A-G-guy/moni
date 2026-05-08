@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agguy.moni.R
import com.agguy.moni.app.components.SettingsItem
import com.agguy.moni.app.components.SettingsToggleItem
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.ui.settings.ExportDataDialog
import com.agguy.moni.app.ui.settings.ImportConfirmDialog
import com.agguy.moni.core.platform.AutoBackupScheduler
import com.agguy.moni.core.platform.DataStoreHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据管理页面。
 *
 * 整合导出备份、导入恢复、自动备份设置、应用内备份管理四个功能。
 * 导出/导入对话框与 SAF launcher 在页面内部管理，不依赖 MoniApp 顶层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: BackupViewModel,
    dbPath: String,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backups by viewModel.backups.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val inspectResult by viewModel.inspectResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf<File?>(null) }

    // 自动备份状态
    var showFrequencyDialog by remember { mutableStateOf(false) }
    val enabled by DataStoreHelper.autoBackupEnabledFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val frequency by DataStoreHelper.autoBackupFrequencyFlow(context)
        .collectAsStateWithLifecycle(initialValue = "daily")
    val maxCount by DataStoreHelper.autoBackupMaxCountFlow(context)
        .collectAsStateWithLifecycle(initialValue = 7)
    var sliderValue by remember { mutableFloatStateOf(maxCount.toFloat()) }
    LaunchedEffect(maxCount) {
        sliderValue = maxCount.toFloat()
    }
    val copyToExternal by DataStoreHelper.autoBackupCopyToExternalFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val externalUri by DataStoreHelper.autoBackupExternalUriFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)
    val lastBackupTime by DataStoreHelper.autoBackupLastTimeFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(enabled, frequency) {
        AutoBackupScheduler.schedule(context, enabled, frequency)
    }

    val frequencyLabel = when (frequency) {
        "every_launch" -> stringResource(R.string.data_frequency_every_launch)
        "daily" -> stringResource(R.string.data_frequency_daily)
        "weekly" -> stringResource(R.string.data_frequency_weekly)
        "monthly" -> stringResource(R.string.data_frequency_monthly)
        else -> stringResource(R.string.data_frequency_daily)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportToSaf(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importUri = it
            showImportDialog = true
            viewModel.clearInspectResult()
            viewModel.inspectBackupFromUri(it)
        }
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

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is BackupOperationState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is BackupOperationState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.data_export_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = "arrow_back",
                            contentDescription = stringResource(R.string.back),
                            size = 24.dp
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 操作区：导出 / 导入
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    iconName = "cloud_upload",
                    title = stringResource(R.string.data_action_card_export_title),
                    subtitle = stringResource(R.string.data_action_card_export_subtitle),
                    modifier = Modifier.weight(1f),
                    onClick = { showExportDialog = true }
                )
                ActionCard(
                    iconName = "cloud_download",
                    title = stringResource(R.string.data_action_card_import_title),
                    subtitle = stringResource(R.string.data_action_card_import_subtitle),
                    modifier = Modifier.weight(1f),
                    onClick = { importLauncher.launch(arrayOf("application/zip")) }
                )
            }

            // 自动备份设置（直接展开）
            SettingsToggleItem(
                iconName = "archive",
                title = stringResource(R.string.data_auto_backup_title),
                subtitle = if (enabled) stringResource(R.string.data_auto_backup_subtitle_on) else stringResource(R.string.data_auto_backup_subtitle_off),
                checked = enabled,
                onCheckedChange = {
                    scope.launch { DataStoreHelper.saveAutoBackupEnabled(context, it) }
                }
            )

            AnimatedVisibility(visible = enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsItem(
                        iconName = "event_repeat",
                        title = stringResource(R.string.data_frequency_label),
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
                                stringResource(R.string.data_max_count_label),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(R.string.data_max_count_unit, sliderValue.toInt()),
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
                            stringResource(R.string.data_max_count_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    SettingsToggleItem(
                        iconName = "folder_copy",
                        title = stringResource(R.string.data_copy_external),
                        subtitle = if (copyToExternal) stringResource(R.string.data_copy_external_on) else stringResource(R.string.data_copy_external_off),
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
                            iconName = "folder_open",
                            title = stringResource(R.string.data_select_external),
                            subtitle = externalUri ?: stringResource(R.string.data_external_not_selected),
                            onClick = { externalDirPickerLauncher.launch(null) }
                        )
                    }

                    val lastBackup = lastBackupTime
                    if (lastBackup != null) {
                        Text(
                            stringResource(R.string.data_last_backup, lastBackup),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = operationState is BackupOperationState.Running) {
                val running = operationState as? BackupOperationState.Running
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        running?.stage ?: stringResource(R.string.data_processing),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { (running?.percent ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 应用内备份（底部，不做内部滚动）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.data_internal_backups, backups.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (backups.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.data_no_backups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    backups.forEach { item ->
                        BackupItemCard(
                            item = item,
                            onRestore = { showRestoreConfirm = item.file },
                            onDelete = { viewModel.deleteBackup(item.file) },
                            onShare = { viewModel.shareBackup(item.file) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 导出数据对话框
    if (showExportDialog) {
        val timestamp = formatBackupTimestamp()
        ExportDataDialog(
            operationState = operationState,
            onExportToInternal = {
                viewModel.exportToInternal()
            },
            onExportToSaf = {
                exportLauncher.launch("Moni_Backup_$timestamp.zip")
            },
            onDismiss = {
                showExportDialog = false
                viewModel.resetState()
            }
        )
    }

    // 导入确认对话框
    if (showImportDialog) {
        ImportConfirmDialog(
            inspectResult = inspectResult,
            operationState = operationState,
            onConfirm = {
                importUri?.let { uri ->
                    viewModel.importFromSaf(uri, dbPath)
                }
            },
            onDismiss = {
                showImportDialog = false
                importUri = null
                viewModel.clearInspectResult()
                viewModel.resetState()
            }
        )
    }

    // 恢复确认对话框
    showRestoreConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text(stringResource(R.string.data_confirm_restore)) },
            text = {
                Text(stringResource(R.string.data_restore_confirm_message, file.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = null
                        viewModel.restoreFromInternal(file, dbPath)
                    }
                ) { Text(stringResource(R.string.action_restore), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // 频率选择对话框
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
private fun ActionCard(
    iconName: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SymbolIcon(
                name = iconName,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 24.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BackupItemCard(
    item: BackupItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.file.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatFileSize(item.file.length()) + " · " +
                            formatFileDateTime(item.file.lastModified()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onShare) {
                    Text(stringResource(R.string.action_share), style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onDelete) {
                    SymbolIcon(name = "delete", contentDescription = stringResource(R.string.delete), size = 24.dp)
                }
            }
            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.action_restore_this))
            }
        }
    }
}

@Composable
private fun FrequencyPickerDialog(
    currentFrequency: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        "every_launch" to stringResource(R.string.data_frequency_every_launch),
        "daily" to stringResource(R.string.data_frequency_daily),
        "weekly" to stringResource(R.string.data_frequency_weekly),
        "monthly" to stringResource(R.string.data_frequency_monthly)
    )
    var selected by remember { mutableStateOf(currentFrequency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.data_select_frequency_title)) },
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
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(Locale.ROOT, "%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatBackupTimestamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}

private fun formatFileDateTime(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
