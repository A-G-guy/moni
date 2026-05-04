@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.dev

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.MockPreset

/**
 * Mock 数据生成对话框。
 */
@Composable
fun DevMockDataDialog(
    onConfirm: (count: Int, preset: MockPreset) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(10) }
    var selectedPreset by remember { mutableStateOf(MockPreset.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("生成测试数据") },
        text = {
            Column {
                Text(
                    "数据量",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                CountOption(
                    label = "10 条",
                    selected = selectedCount == 10,
                    onClick = { selectedCount = 10 }
                )
                CountOption(
                    label = "100 条",
                    selected = selectedCount == 100,
                    onClick = { selectedCount = 100 }
                )
                CountOption(
                    label = "500 条",
                    selected = selectedCount == 500,
                    onClick = { selectedCount = 500 }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "场景预设",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                PresetOption(
                    label = "正常数据",
                    selected = selectedPreset == MockPreset.NORMAL,
                    onClick = { selectedPreset = MockPreset.NORMAL }
                )
                PresetOption(
                    label = "极限溢出（超长文本、超大金额等）",
                    selected = selectedPreset == MockPreset.STRESS,
                    onClick = { selectedPreset = MockPreset.STRESS }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCount, selectedPreset) }
            ) {
                Text("生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CountOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun PresetOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
