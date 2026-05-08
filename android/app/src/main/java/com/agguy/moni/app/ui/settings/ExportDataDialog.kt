@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * 数据备份对话框。
 *
 * 提供"导出到应用内"和"导出到指定目录"两个选项。
 */
@Composable
fun ExportDataDialog(
    operationState: BackupOperationState,
    onExportToInternal: () -> Unit,
    onExportToSaf: () -> Unit,
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
                    isRunning -> stringResource(R.string.data_export_running)
                    operationState is BackupOperationState.Success -> stringResource(R.string.data_export_success)
                    operationState is BackupOperationState.Error -> stringResource(R.string.data_export_failed)
                    else -> stringResource(R.string.data_export_title)
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
                            operationState.message,
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
                    else -> {
                        Text(
                            stringResource(R.string.data_export_description),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                isRunning -> {}
                operationState is BackupOperationState.Success ||
                    operationState is BackupOperationState.Error -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
                else -> {
                    Column {
                        TextButton(
                            onClick = onExportToInternal,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.data_export_internal)) }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onExportToSaf,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.data_export_external)) }
                    }
                }
            }
        },
        dismissButton = {
            if (!isRunning && operationState !is BackupOperationState.Success
                && operationState !is BackupOperationState.Error) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
