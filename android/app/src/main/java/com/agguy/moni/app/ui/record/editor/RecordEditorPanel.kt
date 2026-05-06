@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agguy.moni.app.components.AutoResizeText
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.core.util.formatAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 底部综合控制面板。
 *
 * 布局：
 * - 金额显示栏（居中单独一行）
 * - 信息栏（日期 / 时间 / 备注）
 * - 自定义数字键盘 / 备注编辑留白
 */
@Composable
fun RecordEditorPanel(
    state: RecordEditorState,
    currencySymbol: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onNoteClick: () -> Unit,
    onNoteDone: () -> Unit,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onCalculate: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 第一行：金额（居中）
        AmountDisplay(
            state = state,
            currencySymbol = currencySymbol
        )

        // 第二行：日期、时间、备注
        InfoRow(
            state = state,
            onDateClick = onDateClick,
            onTimeClick = onTimeClick,
            onNoteClick = onNoteClick,
            onNoteDone = onNoteDone
        )

        // 键盘 / 备注编辑留白 切换
        AnimatedContent(
            targetState = state.isNoteEditing,
            transitionSpec = {
                slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut()
            },
            label = "keyboard_note_switch",
            modifier = Modifier.weight(1f)
        ) { isEditing ->
            if (isEditing) {
                // 备注编辑模式：下方留白，点击空白处关闭
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onNoteDone() }
                )
            } else {
                CustomNumPad(
                    recordType = state.recordType,
                    amountExpression = state.amountExpression,
                    amountCents = state.confirmedAmountCents,
                    hasSelectedCategory = state.selectedCategoryId != -1L,
                    onDigitClick = onDigitClick,
                    onOperatorClick = onOperatorClick,
                    onBackspace = onBackspace,
                    onCalculate = onCalculate,
                    onSave = onSave,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun AmountDisplay(
    state: RecordEditorState,
    currencySymbol: String
) {
    val displayAmount = if (state.confirmedAmountCents > 0) {
        currencySymbol + formatAmount(state.confirmedAmountCents)
    } else {
        "$currencySymbol${state.amountExpression.ifEmpty { "0" }}"
    }

    val amountColor = if (state.confirmedAmountCents == 0L) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AutoResizeText(
            text = displayAmount,
            maxFontSize = 48.sp,
            minFontSize = 24.sp,
            style = MaterialTheme.typography.displayMediumEmphasized,
            color = amountColor
        )
    }
}

@Composable
private fun InfoRow(
    state: RecordEditorState,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onNoteClick: () -> Unit,
    onNoteDone: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochSecond(state.timestamp).atZone(zone).toLocalDateTime()
    val localDate = dateTime.toLocalDate()
    val today = LocalDate.now()

    val dateText = when {
        localDate == today -> "今天"
        localDate == today.minusDays(1) -> "昨天"
        else -> localDate.format(DateTimeFormatter.ofPattern("MM月dd日"))
    }

    val timeText = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }

    // 备注编辑时自动请求焦点
    androidx.compose.runtime.LaunchedEffect(state.isNoteEditing) {
        if (state.isNoteEditing) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：日期 + 时间
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TextButton(
                onClick = onDateClick,
                modifier = Modifier.height(36.dp)
            ) {
                MoniIcon(
                    icon = MoniIcons.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            TextButton(
                onClick = onTimeClick,
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // 中间留白
        Spacer(modifier = Modifier.weight(1f))

        // 右侧：备注区域
        if (state.isNoteEditing) {
            BasicTextField(
                value = state.note,
                onValueChange = { state.updateNote(it) },
                modifier = Modifier
                    .weight(2f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onNoteDone() }),
                decorationBox = { innerTextField ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            MoniIcon(
                                icon = MoniIcons.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            ) {
                                innerTextField()
                            }
                            IconButton(
                                onClick = onNoteDone,
                                modifier = Modifier.size(24.dp)
                            ) {
                                MoniIcon(
                                    icon = MoniIcons.Check,
                                    contentDescription = "完成",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )
        } else {
            TextButton(
                onClick = onNoteClick,
                modifier = Modifier.height(36.dp)
            ) {
                MoniIcon(
                    icon = MoniIcons.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = state.note.ifEmpty { "备注" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.note.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
