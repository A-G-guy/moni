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
import androidx.compose.ui.unit.dp
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
                    isRunning -> "正在恢复"
                    operationState is BackupOperationState.Success -> "恢复成功"
                    operationState is BackupOperationState.Error -> "恢复失败"
                    else -> "确认导入"
                }
            )
        },
        text = {
            Column {
                when {
                    isRunning -> {
                        Text(
                            running?.stage ?: "处理中...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LinearWavyProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    operationState is BackupOperationState.Success -> {
                        Text(
                            "${operationState.message}\n\n应用即将重启以完成恢复。",
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
                            "备份信息：\n" +
                                "• 版本：${inspectResult.appVersionName}\n" +
                                "• 创建时间：${inspectResult.createdAt}\n" +
                                "• 记录：${inspectResult.recordCount} 条\n" +
                                "• 分类：${inspectResult.categoryCount} 个\n" +
                                "• 设置：${inspectResult.settingsCount} 项\n\n" +
                                "警告：导入将覆盖所有现有数据，此操作不可撤销。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Text(
                            "正在读取备份信息...",
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
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                operationState is BackupOperationState.Error -> {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                else -> {
                    TextButton(
                        onClick = onConfirm,
                        enabled = inspectResult != null
                    ) { Text("确认导入", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        dismissButton = {
            if (!isRunning && operationState !is BackupOperationState.Success) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
