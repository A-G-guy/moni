@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agguy.moni.app.components.AutoResizeText
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.util.formatAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 底部综合控制面板。
 *
 * 上半：金额显示栏（分类信息 + 日期/备注 + 金额大数字）
 * 下半：自定义数字键盘 / 备注输入切换
 */
@Composable
fun RecordEditorPanel(
    state: RecordEditorState,
    categories: List<CoreCategory>,
    currencySymbol: String,
    onDateClick: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 上半：金额显示栏
        AmountDisplayBar(
            state = state,
            categories = categories,
            currencySymbol = currencySymbol,
            onDateClick = onDateClick,
            onNoteClick = onNoteClick
        )

        // 下半：键盘 / 备注切换
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
                NoteInputField(
                    value = state.note,
                    onValueChange = { state.updateNote(it) },
                    onDone = onNoteDone
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
private fun AmountDisplayBar(
    state: RecordEditorState,
    categories: List<CoreCategory>,
    currencySymbol: String,
    onDateClick: () -> Unit,
    onNoteClick: () -> Unit
) {
    val selectedCategory = state.selectedCategory(categories)
    val categoryName = state.selectedCategoryFullName(categories)
    val iconRes = selectedCategory?.let { iconNameToRes(it.iconName) }

    val dateText = run {
        val date = Instant.ofEpochSecond(state.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        when {
            date == today -> "今天"
            date == today.minusDays(1) -> "昨天"
            else -> date.format(DateTimeFormatter.ofPattern("MM月dd日"))
        }
    }

    val amountColor = when {
        state.confirmedAmountCents == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
        state.recordType == RecordType.EXPENSE -> MaterialTheme.colorScheme.expenseRed
        else -> MaterialTheme.colorScheme.incomeGreen
    }

    val displayAmount = if (state.confirmedAmountCents > 0) {
        currencySymbol + formatAmount(state.confirmedAmountCents)
    } else {
        "$currencySymbol${state.amountExpression.ifEmpty { "0" }}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧：分类 + 日期/备注
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 分类信息
            if (categoryName != null && iconRes != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            MoniIcon(
                                icon = iconRes,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = "选择分类",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 日期 + 备注按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDateClick,
                    modifier = Modifier.height(32.dp)
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
                    onClick = onNoteClick,
                    modifier = Modifier.height(32.dp)
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
                        }
                    )
                }
            }
        }

        // 右侧：金额大数字
        AutoResizeText(
            text = displayAmount,
            maxFontSize = 48.sp,
            minFontSize = 20.sp,
            style = MaterialTheme.typography.displayMediumEmphasized,
            color = amountColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun NoteInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("添加备注...") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = onDone) {
                MoniIcon(
                    icon = MoniIcons.Check,
                    contentDescription = "完成"
                )
            }
        }
    )
}
