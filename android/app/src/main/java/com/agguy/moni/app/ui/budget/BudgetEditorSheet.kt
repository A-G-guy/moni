@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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

private fun centsToInputText(cents: Long): String {
    val yuan = cents / 100
    val fen = kotlin.math.abs(cents % 100)
    return if (fen == 0L) {
        "$yuan"
    } else {
        "$yuan.${fen.toString().padStart(2, '0')}"
    }
}

private fun inputTextToCents(text: String): Long {
    if (text.isEmpty() || text == ".") return 0
    val value = text.toDoubleOrNull() ?: return 0
    return (value * 100).toLong()
}

@Composable
fun BudgetEditorSheet(
    budget: CoreBudget?,
    categoryId: Long?,
    categoryName: String,
    parentBudget: CoreBudget?,
    yearMonth: String,
    onDispatch: (CoreIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    val initKey = remember { java.util.UUID.randomUUID().toString() }
    var amountText by remember(initKey) {
        mutableStateOf(
            budget?.amountCents?.let { centsToInputText(it) } ?: ""
        )
    }

    // 默认 scope：编辑快照=仅本月，编辑模板/新建=本月及以后
    val defaultScope = when {
        budget?.isSnapshot == true -> "this_month"
        budget != null -> "this_and_future"
        else -> "this_and_future"
    }
    var selectedScope by remember(initKey) { mutableStateOf(defaultScope) }
    var deleteConfirmVisible by remember { mutableStateOf(false) }

    val amountCents = inputTextToCents(amountText)
    val isValid = amountCents > 0

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
            Text(
                text = if (budget == null) "设置预算" else "编辑预算",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
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

            // 预算范围选择
            Text(
                text = "生效范围",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                overflowIndicator = {}
            ) {
                toggleableItem(
                    checked = selectedScope == "this_month",
                    label = "仅本月",
                    onCheckedChange = { if (it) selectedScope = "this_month" },
                    weight = 1f
                )
                toggleableItem(
                    checked = selectedScope == "this_and_future",
                    label = "本月及以后",
                    onCheckedChange = { if (it) selectedScope = "this_and_future" },
                    weight = 1f
                )
                toggleableItem(
                    checked = selectedScope == "future_only",
                    label = "仅以后",
                    onCheckedChange = { if (it) selectedScope = "future_only" },
                    weight = 1f
                )
            }

            // 软冲突提示
            if (parentBudget != null && amountCents > 0) {
                BudgetSoftConflictWarning(
                    childAmount = amountCents,
                    parentAmount = parentBudget.amountCents
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (budget != null) {
                    TextButton(
                        onClick = { deleteConfirmVisible = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.expenseRed)
                    }
                }

                Button(
                    onClick = {
                        val cents = inputTextToCents(amountText)
                        if (cents > 0) {
                            // 新建预算时 budget 为 null，需用传入的 categoryId；编辑时两者一致
                            val effectiveCategoryId = categoryId ?: budget?.categoryId
                            onDispatch(
                                CoreIntent.BudgetUpsert(
                                    categoryId = effectiveCategoryId,
                                    amountCents = cents,
                                    yearMonth = yearMonth,
                                    scope = selectedScope
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

    // 删除确认对话框
    if (deleteConfirmVisible && budget != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmVisible = false },
            title = { Text("删除预算") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请选择删除范围：")
                    TextButton(
                        onClick = {
                            onDispatch(
                                CoreIntent.BudgetDelete(
                                    id = budget.id,
                                    yearMonth = yearMonth,
                                    scope = "this_month"
                                )
                            )
                            deleteConfirmVisible = false
                            onDismiss()
                        }
                    ) {
                        Text("从本月起停止")
                    }
                    TextButton(
                        onClick = {
                            onDispatch(
                                CoreIntent.BudgetDelete(
                                    id = budget.id,
                                    yearMonth = yearMonth,
                                    scope = "future_only"
                                )
                            )
                            deleteConfirmVisible = false
                            onDismiss()
                        }
                    ) {
                        Text("从下月起停止")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { deleteConfirmVisible = false }) {
                    Text("取消")
                }
            }
        )
    }
}

