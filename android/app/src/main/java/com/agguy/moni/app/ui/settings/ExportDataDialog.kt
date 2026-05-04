@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.ExportFormat
import kotlinx.coroutines.delay

private const val EXPORT_VISUAL_FEEDBACK_MS = 600L

/**
 * 数据导出格式选择对话框。
 *
 * Material 3 Expressive 改造点：
 * - 接入 [LinearWavyProgressIndicator]：点击"导出"后显示带波形动画的进度条，
 *   再触发 [onConfirm]，给用户一个 hero moment 的反馈，符合 shape morphing 哲学；
 * - shape 升级到 [androidx.compose.material3.Shapes.extraLarge] (32dp)，与 dialog 视觉体系协同。
 */
@Composable
fun ExportDataDialog(onConfirm: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
    val formats = listOf(
        ExportFormat.CSV to "CSV 格式（表格）",
        ExportFormat.JSON to "JSON 格式（原始数据）"
    )
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(isExporting) {
        if (isExporting) {
            delay(EXPORT_VISUAL_FEEDBACK_MS)
            onConfirm(selectedFormat)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(if (isExporting) "正在导出" else "导出数据") },
        text = {
            Column {
                if (isExporting) {
                    Text(
                        text = "正在准备数据并写入 Download 目录…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "选择导出格式，文件将保存到 Download 目录。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    formats.forEach { (format, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFormat = format }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { isExporting = true },
                enabled = !isExporting
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("取消")
            }
        }
    )
}
