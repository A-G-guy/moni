@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.ui.settings.ExportDataDialog
import com.agguy.moni.app.ui.settings.ImportConfirmDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据管理页面。
 *
 * 整合导出备份、导入恢复、应用内备份管理三个功能。
 * 导出/导入对话框与 SAF launcher 在页面内部管理，不依赖 MoniApp 顶层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: BackupViewModel,
    dbPath: String,
    onNavigateBack: () -> Unit,
    onNavigateToAutoBackupSettings: () -> Unit = {},
) {
    val backups by viewModel.backups.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val inspectResult by viewModel.inspectResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf<File?>(null) }

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
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(MoniIcons.ArrowBack), contentDescription = "返回")
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 操作区：导出 / 导入
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    icon = MoniIcons.Cloud,
                    title = "导出备份",
                    subtitle = "备份到应用内或外部",
                    modifier = Modifier.weight(1f),
                    onClick = { showExportDialog = true }
                )
                ActionCard(
                    icon = MoniIcons.Archive,
                    title = "导入备份",
                    subtitle = "从 ZIP 文件恢复",
                    modifier = Modifier.weight(1f),
                    onClick = { importLauncher.launch(arrayOf("application/zip")) }
                )
            }

            // 自动备份设置入口
            Card(
                onClick = onNavigateToAutoBackupSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(MoniIcons.Archive),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "自动备份",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "定时自动备份到本地或外部目录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "设置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = operationState is BackupOperationState.Running) {
                val running = operationState as? BackupOperationState.Running
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        running?.stage ?: "处理中...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { (running?.percent ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "应用内备份 (${backups.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (backups.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "暂无应用内备份",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(backups, key = { it.file.absolutePath }) { item ->
                        BackupItemCard(
                            item = item,
                            onRestore = { showRestoreConfirm = item.file },
                            onDelete = { viewModel.deleteBackup(item.file) },
                            onShare = { viewModel.shareBackup(item.file) }
                        )
                    }
                }
            }
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
            title = { Text("确认恢复") },
            text = {
                Text("将从 ${file.name} 恢复数据。当前所有数据将被覆盖，此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = null
                        viewModel.restoreFromInternal(file, dbPath)
                    }
                ) { Text("恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ActionCard(
    icon: Int,
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
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                    Text("分享", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onDelete) {
                    Icon(painterResource(MoniIcons.Delete), contentDescription = "删除")
                }
            }
            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("恢复此备份")
            }
        }
    }
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
