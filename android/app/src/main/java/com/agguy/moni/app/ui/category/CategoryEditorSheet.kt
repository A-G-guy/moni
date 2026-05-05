@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import androidx.compose.foundation.layout.Box
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName

/**
 * 分类编辑器 BottomSheet。
 *
 * 统一处理「新增」与「编辑」两种模式：
 * - 新增：所有字段可编辑，type 默认由外部传入（跟随当前 Tab）。
 * - 编辑：所有字段均可编辑（预设分类不再锁定）。
 * - 颜色由 type 决定（expense=红 / income=绿），不提供颜色选择器。
 *
 * @param category 待编辑的分类；null 表示新增模式
 * @param defaultType 新增时的默认类型；编辑模式下由 [category] 决定
 * @param onDispatch 意图分发回调
 * @param onDismiss 关闭 Sheet 回调
 */
@Composable
fun CategoryEditorSheet(
    category: CoreCategory?,
    defaultType: RecordType,
    categories: List<CoreCategory>,
    onDispatch: (CoreIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = category != null

    val initialType = if (category != null) {
        if (category.categoryType == RecordType.INCOME.serialName) RecordType.INCOME else RecordType.EXPENSE
    } else {
        defaultType
    }

    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedIconName by remember {
        mutableStateOf(category?.iconName ?: "restaurant")
    }
    var selectedParentId by remember { mutableStateOf(category?.parentId) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showParentPicker by remember { mutableStateOf(false) }

    val typeColor = when (selectedType) {
        RecordType.EXPENSE -> MaterialTheme.colorScheme.expenseRed
        RecordType.INCOME -> MaterialTheme.colorScheme.incomeGreen
    }

    val isNameValid = name.isNotBlank()
    val isDescriptionValid = description.length <= DESCRIPTION_MAX_LENGTH
    val isSaveEnabled = isNameValid && isDescriptionValid

    // 编辑模式下，判断该分类是否已有子分类
    val hasChildren = remember(category, categories) {
        isEditMode && categories.any { it.parentId == category!!.id }
    }

    // 可选的父分类列表（同类型、一级分类、非自身、未归档）
    val availableParents = remember(selectedType, categories, category) {
        categories.filter {
            it.parentId == null
                && it.categoryType == selectedType.serialName
                && it.id != category?.id
                && it.archivedAt == null
        }
    }

    val selectedParent = remember(selectedParentId, categories) {
        categories.find { it.id == selectedParentId }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题
            Text(
                text = if (isEditMode) "编辑分类" else "添加分类",
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 类型选择器
            CategoryTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                modifier = Modifier.fillMaxWidth()
            )

            // 名称输入
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    focusedLabelColor = typeColor
                )
            )

            // 描述输入
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= DESCRIPTION_MAX_LENGTH) description = it
                },
                label = { Text("描述") },
                placeholder = { Text("可选，最多 $DESCRIPTION_MAX_LENGTH 字") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    focusedLabelColor = typeColor
                ),
                supportingText = {
                    Text(
                        text = "${description.length} / $DESCRIPTION_MAX_LENGTH",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )

            // 父分类选择行（仅当该分类没有子分类时显示）
            if (!hasChildren) {
                ParentCategorySelectorRow(
                    parent = selectedParent,
                    onClick = { showParentPicker = true },
                    onClear = { selectedParentId = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 图标选择行
            IconSelectorRow(
                iconName = selectedIconName,
                onClick = { showIconPicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (!isSaveEnabled) return@Button
                    val trimmedName = name.trim()
                    val trimmedDescription = description.trim().takeIf { it.isNotBlank() }

                    if (isEditMode) {
                        val originalParentId = category.parentId
                        val clearParentId = originalParentId != null && selectedParentId == null
                        onDispatch(
                            CoreIntent.CategoryUpdate(
                                id = category.id,
                                name = trimmedName,
                                description = trimmedDescription,
                                iconName = selectedIconName,
                                parentId = selectedParentId,
                                clearParentId = clearParentId
                            )
                        )
                    } else {
                        onDispatch(
                            CoreIntent.CategoryCreate(
                                name = trimmedName,
                                description = trimmedDescription,
                                categoryType = selectedType,
                                iconName = selectedIconName,
                                parentId = selectedParentId
                            )
                        )
                    }
                    onDismiss()
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "保存",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }

    // 图标选择器 BottomSheet
    if (showIconPicker) {
        IconPickerSheet(
            selectedIconName = selectedIconName,
            onIconSelected = { selectedIconName = it },
            onDismiss = { showIconPicker = false }
        )
    }

    // 父分类选择器 BottomSheet
    if (showParentPicker) {
        ParentCategoryPickerSheet(
            parents = availableParents,
            selectedParentId = selectedParentId,
            onParentSelected = { selectedParentId = it },
            onDismiss = { showParentPicker = false }
        )
    }
}

/**
 * 收入/支出类型切换器。
 *
 * 使用 Material 3 Expressive [ButtonGroup] + [toggleableItem]，
 * 选中时邻居自动 squish 形变。
 */
@Composable
private fun CategoryTypeSelector(
    selectedType: RecordType,
    onTypeSelected: (RecordType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "类型",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ButtonGroup(
            overflowIndicator = { /* 仅 2 个固定项，永不溢出 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            toggleableItem(
                checked = selectedType == RecordType.EXPENSE,
                label = "支出",
                onCheckedChange = { if (it) onTypeSelected(RecordType.EXPENSE) },
                icon = {
                    if (selectedType == RecordType.EXPENSE) {
                        MoniIcon(
                            MoniIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                weight = 1f
            )
            toggleableItem(
                checked = selectedType == RecordType.INCOME,
                label = "收入",
                onCheckedChange = { if (it) onTypeSelected(RecordType.INCOME) },
                icon = {
                    if (selectedType == RecordType.INCOME) {
                        MoniIcon(
                            MoniIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                weight = 1f
            )
        }
    }
}

/**
 * 图标选择触发行。
 *
 * 左侧显示当前图标，中间显示名称，右侧箭头提示可点击。
 */
@Composable
private fun IconSelectorRow(
    iconName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "图标",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MoniIcon(
                    icon = iconNameToRes(iconName),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = iconName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                MoniIcon(
                    icon = MoniIcons.ExpandMore,
                    contentDescription = "选择图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 父分类选择触发行。
 *
 * 左侧显示当前父分类图标和名称，右侧提供清除按钮。
 */
@Composable
private fun ParentCategorySelectorRow(
    parent: CoreCategory?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "父分类",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (parent != null) {
                    val parentColor = if (parent.categoryType == "expense")
                        MaterialTheme.colorScheme.expenseRed
                    else
                        MaterialTheme.colorScheme.incomeGreen

                    MoniIcon(
                        icon = iconNameToRes(parent.iconName),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = parentColor
                    )

                    Text(
                        text = parent.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // 清除按钮
                    TextButton(onClick = onClear) {
                        Text(
                            text = "清除",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "作为一级分类（不选父分类）",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                MoniIcon(
                    icon = MoniIcons.ExpandMore,
                    contentDescription = "选择父分类",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val DESCRIPTION_MAX_LENGTH = 200
