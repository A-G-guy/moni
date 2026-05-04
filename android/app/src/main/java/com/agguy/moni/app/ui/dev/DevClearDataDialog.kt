@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.dev

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.agguy.moni.app.theme.expenseRed

/**
 * 清空数据二次确认对话框。
 */
@Composable
fun DevClearDataDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("危险操作确认") },
        text = {
            Text(
                "此操作将清空所有记账数据和应用设置，不可恢复。\n\n" +
                    "应用将自动重启并回到初始状态。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "确认清空",
                    color = MaterialTheme.colorScheme.expenseRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
