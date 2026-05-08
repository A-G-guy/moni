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
import androidx.compose.ui.res.stringResource
import com.agguy.moni.R
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.BudgetScope

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
    val parts = text.split(".")
    val yuan = parts[0].toLongOrNull() ?: return 0
    val fen = if (parts.size > 1) {
        parts[1].padEnd(2, '0').take(2).toLongOrNull() ?: 0
    } else {
        0
    }
    return yuan * 100 + fen
}

@Composable
fun BudgetEditorSheet(
    budget: CoreBudget?,
    categoryId: Long?,
    categoryName: String,
    parentBudget: CoreBudget?,
    yearMonth: String,
    currencySymbol: String,
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
        budget?.isSnapshot == true -> BudgetScope.THIS_MONTH
        budget != null -> BudgetScope.THIS_AND_FUTURE
        else -> BudgetScope.THIS_AND_FUTURE
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
                text = if (budget == null) stringResource(R.string.budget_set) else stringResource(R.string.editor_title_edit),
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
                label = { Text(stringResource(R.string.budget_amount)) },
                prefix = { Text(currencySymbol) },
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
                text = stringResource(R.string.label_scope),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val scopeThisMonth = stringResource(R.string.budget_scope_this_month)
            val scopeThisAndFuture = stringResource(R.string.budget_scope_this_and_future)
            val scopeFutureOnly = stringResource(R.string.budget_scope_future_only)
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                overflowIndicator = {}
            ) {
                toggleableItem(
                    checked = selectedScope == BudgetScope.THIS_MONTH,
                    label = scopeThisMonth,
                    onCheckedChange = { if (it) selectedScope = BudgetScope.THIS_MONTH },
                    weight = 1f
                )
                toggleableItem(
                    checked = selectedScope == BudgetScope.THIS_AND_FUTURE,
                    label = scopeThisAndFuture,
                    onCheckedChange = { if (it) selectedScope = BudgetScope.THIS_AND_FUTURE },
                    weight = 1f
                )
                toggleableItem(
                    checked = selectedScope == BudgetScope.FUTURE_ONLY,
                    label = scopeFutureOnly,
                    onCheckedChange = { if (it) selectedScope = BudgetScope.FUTURE_ONLY },
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
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.expenseRed)
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
                    Text(stringResource(R.string.save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 删除确认对话框
    if (deleteConfirmVisible && budget != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmVisible = false },
            title = { Text(stringResource(R.string.budget_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.budget_delete_message))
                    TextButton(
                        onClick = {
                            onDispatch(
                                CoreIntent.BudgetDelete(
                                    id = budget.id,
                                    yearMonth = yearMonth,
                                    scope = BudgetScope.THIS_MONTH
                                )
                            )
                            deleteConfirmVisible = false
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.budget_stop_this_month))
                    }
                    TextButton(
                        onClick = {
                            onDispatch(
                                CoreIntent.BudgetDelete(
                                    id = budget.id,
                                    yearMonth = yearMonth,
                                    scope = BudgetScope.FUTURE_ONLY
                                )
                            )
                            deleteConfirmVisible = false
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.budget_stop_next_month))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { deleteConfirmVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

