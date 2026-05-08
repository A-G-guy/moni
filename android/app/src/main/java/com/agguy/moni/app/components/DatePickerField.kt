package com.agguy.moni.app.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.icons.SymbolIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日期选择字段。
 *
 * 升级到 Material 3 Expressive：使用 medium 圆角与 [FilledTonalIconButton] trailing icon，
 * 视觉上与新的 corner token 体系（large=20、extraLarge=32）保持一致。
 *
 * @param timestamp 当前日期时间戳（Unix 秒）
 * @param onTimestampChange 日期变化回调（Unix 秒）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(timestamp: Long, onTimestampChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val displayDate = remember(timestamp) { formatDate(timestamp) }

    OutlinedTextField(
        value = displayDate,
        onValueChange = { },
        label = { Text(stringResource(R.string.label_date)) },
        readOnly = true,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        trailingIcon = {
            FilledTonalIconButton(onClick = { showDialog = true }) {
                SymbolIcon(name = "event", contentDescription = stringResource(R.string.editor_select_date), size = 24.dp)
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = timestamp * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onTimestampChange(millis / 1000)
                        }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDate(timestamp: Long): String = try {
    Instant.ofEpochSecond(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
} catch (e: Exception) {
    Log.w("Moni", "Date format failed: timestamp=$timestamp, ${e.message}")
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}
