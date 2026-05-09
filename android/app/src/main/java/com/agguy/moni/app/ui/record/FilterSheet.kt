@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class
)

package com.agguy.moni.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.agguy.moni.R
import com.agguy.moni.app.SearchParams
import com.agguy.moni.app.components.DatePickerField
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.core.CoreCategory

/**
 * 筛选条件面板。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    categories: List<CoreCategory>,
    currentParams: SearchParams,
    onApply: (SearchParams) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempParams by remember(currentParams) { mutableStateOf(currentParams) }

    // 过滤未归档分类
    val activeCategories = remember(categories) {
        categories.filter { it.archivedAt == null }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    tempParams = SearchParams()
                    onReset()
                }) {
                    Text(stringResource(R.string.filter_reset))
                }
            }

            // 记录类型
            FilterSection(title = stringResource(R.string.label_type)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeFilterChip(
                        label = stringResource(R.string.filter_type_all),
                        selected = tempParams.recordType == null,
                        onClick = { tempParams = tempParams.copy(recordType = null) }
                    )
                    TypeFilterChip(
                        label = stringResource(R.string.record_expense),
                        selected = tempParams.recordType == "expense",
                        onClick = { tempParams = tempParams.copy(recordType = "expense") }
                    )
                    TypeFilterChip(
                        label = stringResource(R.string.record_income),
                        selected = tempParams.recordType == "income",
                        onClick = { tempParams = tempParams.copy(recordType = "income") }
                    )
                }
            }

            // 分类多选
            if (activeCategories.isNotEmpty()) {
                FilterSection(title = stringResource(R.string.record_category)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeCategories.forEach { category ->
                            val isSelected = tempParams.categoryIds.contains(category.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newIds = if (isSelected) {
                                        tempParams.categoryIds - category.id
                                    } else {
                                        tempParams.categoryIds + category.id
                                    }
                                    tempParams = tempParams.copy(categoryIds = newIds)
                                },
                                label = { Text(category.name) },
                                leadingIcon = if (isSelected) {
                                    {
                                        SymbolIcon(
                                            name = "check",
                                            contentDescription = null,
                                            size = 18.dp
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // 金额范围
            FilterSection(title = stringResource(R.string.record_amount)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tempParams.amountMin?.toString() ?: "",
                        onValueChange = {
                            tempParams = tempParams.copy(
                                amountMin = it.toLongOrNull()?.takeIf { v -> v >= 0 }
                            )
                        },
                        label = { Text(stringResource(R.string.filter_amount_min)) },
                        prefix = { Text(stringResource(R.string.currency_cny_prefix)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text("~", style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = tempParams.amountMax?.toString() ?: "",
                        onValueChange = {
                            tempParams = tempParams.copy(
                                amountMax = it.toLongOrNull()?.takeIf { v -> v >= 0 }
                            )
                        },
                        label = { Text(stringResource(R.string.filter_amount_max)) },
                        prefix = { Text(stringResource(R.string.currency_cny_prefix)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 时间范围
            FilterSection(title = stringResource(R.string.record_date)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePickerField(
                        timestamp = tempParams.dateStart ?: java.time.LocalDate.now()
                            .minusMonths(3).toEpochDay() * 86400,
                        onTimestampChange = {
                            tempParams = tempParams.copy(dateStart = it)
                        }
                    )
                    DatePickerField(
                        timestamp = tempParams.dateEnd
                            ?: java.time.LocalDate.now().toEpochDay() * 86400,
                        onTimestampChange = {
                            tempParams = tempParams.copy(dateEnd = it)
                        }
                    )
                }
            }

            // 排序
            FilterSection(title = stringResource(R.string.sort)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortFilterChip(
                            label = stringResource(R.string.sort_by_time),
                            selected = tempParams.sortBy == "created_at",
                            onClick = { tempParams = tempParams.copy(sortBy = "created_at") }
                        )
                        SortFilterChip(
                            label = stringResource(R.string.sort_by_amount),
                            selected = tempParams.sortBy == "amount_cents",
                            onClick = { tempParams = tempParams.copy(sortBy = "amount_cents") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortFilterChip(
                            label = stringResource(R.string.sort_asc),
                            selected = tempParams.sortOrder == "asc",
                            onClick = { tempParams = tempParams.copy(sortOrder = "asc") }
                        )
                        SortFilterChip(
                            label = stringResource(R.string.sort_desc),
                            selected = tempParams.sortOrder == "desc",
                            onClick = { tempParams = tempParams.copy(sortOrder = "desc") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 确定按钮
            androidx.compose.material3.Button(
                onClick = { onApply(tempParams) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.filter_apply))
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun TypeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SortFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}
