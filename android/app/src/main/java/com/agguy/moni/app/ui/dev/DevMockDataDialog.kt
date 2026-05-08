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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.core.MockPreset

/**
 * Mock 数据生成对话框。
 */
@Composable
fun DevMockDataDialog(onConfirm: (count: Int, preset: MockPreset) -> Unit, onDismiss: () -> Unit) {
    var selectedCount by remember { mutableIntStateOf(10) }
    var selectedPreset by remember { mutableStateOf(MockPreset.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.dev_mock_data_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.dev_mock_data_amount),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                CountOption(
                    label = stringResource(R.string.dev_mock_data_count_10),
                    selected = selectedCount == 10,
                    onClick = { selectedCount = 10 }
                )
                CountOption(
                    label = stringResource(R.string.dev_mock_data_count_100),
                    selected = selectedCount == 100,
                    onClick = { selectedCount = 100 }
                )
                CountOption(
                    label = stringResource(R.string.dev_mock_data_count_500),
                    selected = selectedCount == 500,
                    onClick = { selectedCount = 500 }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.dev_mock_preset),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                PresetOption(
                    label = stringResource(R.string.dev_mock_preset_normal),
                    selected = selectedPreset == MockPreset.NORMAL,
                    onClick = { selectedPreset = MockPreset.NORMAL }
                )
                PresetOption(
                    label = stringResource(R.string.dev_mock_preset_stress),
                    selected = selectedPreset == MockPreset.STRESS,
                    onClick = { selectedPreset = MockPreset.STRESS }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCount, selectedPreset) }
            ) {
                Text(stringResource(R.string.action_generate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CountOption(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun PresetOption(label: String, selected: Boolean, onClick: () -> Unit) {
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
