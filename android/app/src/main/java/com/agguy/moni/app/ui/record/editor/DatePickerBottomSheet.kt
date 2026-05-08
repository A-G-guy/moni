package com.agguy.moni.app.ui.record.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * 底部日期选择面板。
 *
 * [ModalBottomSheet] 包裹 [DatePicker]，选完收起不跳转页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    selectedTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp * 1000
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val seconds = millis / 1000
                            // 保持原时区的 start-of-day
                            val selectedDate = Instant.ofEpochSecond(seconds)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault())
                                .toEpochSecond()
                            onDateSelected(startOfDay)
                        }
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
