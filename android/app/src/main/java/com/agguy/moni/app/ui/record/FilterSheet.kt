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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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

/** 筛选条件面板。 */
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

    val activeCategories = remember(categories) {
        categories.filter { it.archivedAt == null }
    }

    val parentCategories = remember(activeCategories) {
        activeCategories.filter { it.parentId == null }
    }

    val childCategoriesMap = remember(activeCategories) {
        activeCategories
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }
    }

    var selectedParentId by remember { mutableLongStateOf(-1L) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilterSection(title = stringResource(R.string.label_type)) {
                val typeOptions = listOf(
                    stringResource(R.string.filter_type_all) to null,
                    stringResource(R.string.record_expense) to "expense",
                    stringResource(R.string.record_income) to "income"
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    typeOptions.forEachIndexed { index, (label, typeValue) ->
                        val selected = tempParams.recordType == typeValue
                        SegmentedButton(
                            selected = selected,
                            onClick = { tempParams = tempParams.copy(recordType = typeValue) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = typeOptions.size
                            )
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ========== 分类层级选择 ==========
            if (parentCategories.isNotEmpty()) {
                FilterSection(title = stringResource(R.string.record_category)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            item {
                                val isSelected = selectedParentId == -1L
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedParentId = -1L
                                        tempParams = tempParams.copy(categoryIds = emptyList())
                                    },
                                    label = { Text(stringResource(R.string.filter_type_all)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            items(parentCategories) { parent ->
                                val isSelected = selectedParentId == parent.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedParentId = if (isSelected) -1L else parent.id
                                    },
                                    label = { Text(parent.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        if (selectedParentId != -1L) {
                            val children = childCategoriesMap[selectedParentId].orEmpty()
                            if (children.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    children.forEach { child ->
                                        val isSelected = tempParams.categoryIds.contains(child.id)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                val newIds = if (isSelected) {
                                                    tempParams.categoryIds - child.id
                                                } else {
                                                    tempParams.categoryIds + child.id
                                                }
                                                tempParams = tempParams.copy(categoryIds = newIds)
                                            },
                                            label = { Text(child.name) },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    SymbolIcon(
                                                        name = "check",
                                                        contentDescription = null,
                                                        size = 18.dp
                                                    )
                                                }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

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

            FilterSection(title = stringResource(R.string.record_date)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatePickerField(
                        timestamp = tempParams.dateStart ?: java.time.LocalDate.now()
                            .minusMonths(3).toEpochDay() * 86400,
                        onTimestampChange = {
                            tempParams = tempParams.copy(dateStart = it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text("~", style = MaterialTheme.typography.bodyLarge)
                    DatePickerField(
                        timestamp = tempParams.dateEnd
                            ?: java.time.LocalDate.now().toEpochDay() * 86400,
                        onTimestampChange = {
                            tempParams = tempParams.copy(dateEnd = it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            FilterSection(title = stringResource(R.string.sort)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sortByOptions = listOf(
                        stringResource(R.string.sort_by_time) to "created_at",
                        stringResource(R.string.sort_by_amount) to "amount_cents"
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sortByOptions.forEachIndexed { index, (label, value) ->
                            val selected = tempParams.sortBy == value
                            SegmentedButton(
                                selected = selected,
                                onClick = { tempParams = tempParams.copy(sortBy = value) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = sortByOptions.size
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    val sortOrderOptions = listOf(
                        stringResource(R.string.sort_desc) to "desc",
                        stringResource(R.string.sort_asc) to "asc"
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sortOrderOptions.forEachIndexed { index, (label, value) ->
                            val selected = tempParams.sortOrder == value
                            SegmentedButton(
                                selected = selected,
                                onClick = { tempParams = tempParams.copy(sortOrder = value) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = sortOrderOptions.size
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        tempParams = SearchParams()
                        selectedParentId = -1L
                        onReset()
                    },
                    modifier = Modifier.weight(3f)
                ) {
                    Text(stringResource(R.string.filter_reset))
                }
                Button(
                    onClick = { onApply(tempParams) },
                    modifier = Modifier.weight(7f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(stringResource(R.string.filter_apply))
                }
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}