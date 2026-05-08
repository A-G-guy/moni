@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.ui.backup.BackupOperationState

/**
 * 导入确认对话框。
 *
 * 显示备份预览信息并要求用户确认覆盖。
 */
@Composable
fun ImportConfirmDialog(
    inspectResult: uniffi.moni_core.BackupInspection?,
    operationState: BackupOperationState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isRunning = operationState is BackupOperationState.Running
    val running = operationState as? BackupOperationState.Running

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                when {
                    isRunning -> stringResource(R.string.data_import_running)
                    operationState is BackupOperationState.Success -> stringResource(R.string.data_import_success)
                    operationState is BackupOperationState.Error -> stringResource(R.string.data_import_failed)
                    else -> stringResource(R.string.data_import_title)
                }
            )
        },
        text = {
            Column {
                when {
                    isRunning -> {
                        Text(
                            running?.stage ?: stringResource(R.string.data_processing),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LinearWavyProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    operationState is BackupOperationState.Success -> {
                        Text(
                            stringResource(R.string.data_import_success_message, operationState.message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    operationState is BackupOperationState.Error -> {
                        Text(
                            operationState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    inspectResult != null -> {
                        Text(
                            stringResource(
                                R.string.data_backup_info,
                                inspectResult.appVersionName,
                                inspectResult.createdAt,
                                inspectResult.recordCount,
                                inspectResult.categoryCount,
                                inspectResult.settingsCount
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Text(
                            stringResource(R.string.data_import_reading),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                isRunning -> {}
                operationState is BackupOperationState.Success -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
                operationState is BackupOperationState.Error -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
                else -> {
                    TextButton(
                        onClick = onConfirm,
                        enabled = inspectResult != null
                    ) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        dismissButton = {
            if (!isRunning && operationState !is BackupOperationState.Success) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
