package com.agguy.moni.app.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.util.Log
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons

/**
 * 日期选择字段。
 *
 * @param timestamp 当前日期时间戳（Unix 秒）
 * @param onTimestampChange 日期变化回调（Unix 秒）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    timestamp: Long,
    onTimestampChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayDate = remember(timestamp) { formatDate(timestamp) }

    OutlinedTextField(
        value = displayDate,
        onValueChange = { },
        label = { Text("日期") },
        readOnly = true,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                MoniIcon(MoniIcons.Event, contentDescription = "选择日期")
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
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } catch (e: Exception) {
        Log.w("Moni", "日期格式化失败: timestamp=$timestamp, ${e.message}")
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
}
