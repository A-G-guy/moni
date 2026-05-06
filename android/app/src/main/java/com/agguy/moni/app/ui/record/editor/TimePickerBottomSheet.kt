package com.agguy.moni.app.ui.record.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId

/**
 * 底部时间选择面板。
 *
 * [ModalBottomSheet] 包裹 [TimePicker]，选完收起不跳转页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerBottomSheet(
    selectedTimestamp: Long,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentZone = ZoneId.systemDefault()
    val currentLocalTime = Instant.ofEpochSecond(selectedTimestamp)
        .atZone(currentZone)
        .toLocalTime()

    val timePickerState = rememberTimePickerState(
        initialHour = currentLocalTime.hour,
        initialMinute = currentLocalTime.minute,
        is24Hour = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        onDismiss()
                    }
                ) {
                    Text("确定")
                }
            }
        }
    }
}
