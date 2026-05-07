@file:OptIn(ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreIntent

/**
 * 将金额（分）转换为输入框显示的元字符串。
 * 例如 300000 → "3000"，3050 → "30.50"。
 */
private fun centsToInputText(cents: Long): String {
    val yuan = cents / 100
    val fen = kotlin.math.abs(cents % 100)
    return if (fen == 0L) {
        "$yuan"
    } else {
        "$yuan.${fen.toString().padStart(2, '0')}"
    }
}

/**
 * 将输入框的元字符串转换为分。
 * 例如 "3000" → 300000，"30.50" → 3050。
 * 非法输入返回 0。
 */
private fun inputTextToCents(text: String): Long {
    if (text.isEmpty() || text == ".") return 0
    val value = text.toDoubleOrNull() ?: return 0
    return (value * 100).toLong()
}

/**
 * 预算编辑底部弹窗。
 *
 * @param budget 当前预算（null 表示新建）
 * @param categoryName 分类名称（总预算显示"总预算"）
 * @param parentBudget 父级预算（用于软冲突检测）
 * @param onDispatch 意图分发
 * @param onDismiss 关闭回调
 */
@Composable
fun BudgetEditorSheet(
    budget: CoreBudget?,
    categoryName: String,
    parentBudget: CoreBudget?,
    onDispatch: (CoreIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    // 使用独立 key 控制初始化，避免 budget 对象引用变化导致输入丢失
    val initKey = remember { java.util.UUID.randomUUID().toString() }
    var amountText by remember(initKey) {
        mutableStateOf(
            budget?.amountCents?.let { centsToInputText(it) } ?: ""
        )
    }

    val amountCents = inputTextToCents(amountText)
    val isValid = amountCents > 0

    // 弹窗打开后自动聚焦输入框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Text(
                text = if (budget == null) "设置预算" else "编辑预算",
                style = MaterialTheme.typography.titleLarge
            )

            // 分类名称
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 金额输入
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    // 最多一个小数点，且小数点后最多两位
                    val dotIndex = filtered.indexOf('.')
                    val trimmed = if (dotIndex >= 0) {
                        filtered.substring(0, dotIndex + 1) +
                                filtered.substring(dotIndex + 1).filter { it.isDigit() }.take(2)
                    } else {
                        filtered
                    }
                    if (trimmed.count { it == '.' } <= 1) {
                        amountText = trimmed
                    }
                },
                label = { Text("预算金额") },
                prefix = { Text("¥") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )

            // 软冲突提示
            if (parentBudget != null && amountCents > 0) {
                BudgetSoftConflictWarning(
                    childAmount = amountCents,
                    parentAmount = parentBudget.amountCents
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (budget != null) {
                    TextButton(
                        onClick = {
                            onDispatch(CoreIntent.BudgetDelete(id = budget.id))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.expenseRed)
                    }
                }

                Button(
                    onClick = {
                        val cents = inputTextToCents(amountText)
                        if (cents > 0) {
                            onDispatch(
                                CoreIntent.BudgetUpsert(
                                    categoryId = budget?.categoryId,
                                    amountCents = cents
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
