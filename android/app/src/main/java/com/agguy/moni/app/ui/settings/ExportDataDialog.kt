package com.agguy.moni.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.ExportFormat

/**
 * 数据导出格式选择对话框。
 *
 * 让用户选择导出为 CSV 或 JSON 格式。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportDataDialog(
    onConfirm: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf(
        ExportFormat.CSV to "CSV 格式（表格）",
        ExportFormat.JSON to "JSON 格式（原始数据）"
    )
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
    ) {
        AlertDialog(
            onDismissRequest = {
                visible = false
                onDismiss()
            },
            title = { Text("导出数据") },
            text = {
                Column {
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
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(selectedFormat) }) {
                    Text("导出")
                }
            },
            dismissButton = {
                TextButton(onClick = { visible = false; onDismiss() }) {
                    Text("取消")
                }
            }
        )
    }
}
