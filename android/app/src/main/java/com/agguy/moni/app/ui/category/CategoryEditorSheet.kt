@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.AvailableCategoryIcons
import com.agguy.moni.app.theme.expenseRed
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
 * - 编辑：预设分类锁定 name/type；非预设分类全部可编辑。
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
    onDispatch: (CoreIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = category != null
    val isPreset = category?.isPreset == true

    val initialType = if (category != null) {
        if (category.categoryType == RecordType.INCOME.serialName) RecordType.INCOME else RecordType.EXPENSE
    } else defaultType

    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedIconIndex by remember {
        val idx = category?.iconName?.let { iconName ->
            AvailableCategoryIcons.indexOfFirst { it.first == iconName }
        }?.takeIf { it >= 0 } ?: 0
        mutableIntStateOf(idx)
    }

    val typeColor = when (selectedType) {
        RecordType.EXPENSE -> MaterialTheme.colorScheme.expenseRed
        RecordType.INCOME -> MaterialTheme.colorScheme.incomeGreen
    }

    val isNameValid = name.isNotBlank()
    val isDescriptionValid = description.length <= DESCRIPTION_MAX_LENGTH
    val isSaveEnabled = isNameValid && isDescriptionValid

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

            // 类型选择器（编辑预设分类时禁用）
            CategoryTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                enabled = !isPreset,
                modifier = Modifier.fillMaxWidth()
            )

            // 名称输入
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                enabled = !isPreset,
                readOnly = isPreset,
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

            // 图标选择
            IconSelector(
                selectedIndex = selectedIconIndex,
                onIconSelected = { selectedIconIndex = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (!isSaveEnabled) return@Button
                    val (iconName, _) = AvailableCategoryIcons[selectedIconIndex]
                    val trimmedName = name.trim()
                    val trimmedDescription = description.trim().takeIf { it.isNotBlank() }

                    if (isEditMode) {
                        onDispatch(
                            CoreIntent.CategoryUpdate(
                                id = category.id,
                                name = if (isPreset) null else trimmedName,
                                description = trimmedDescription,
                                iconName = iconName
                            )
                        )
                    } else {
                        onDispatch(
                            CoreIntent.CategoryCreate(
                                name = trimmedName,
                                description = trimmedDescription,
                                categoryType = selectedType,
                                iconName = iconName
                            )
                        )
                    }
                    onDismiss()
                },
                enabled = isSaveEnabled,
                contentPadding = ButtonDefaults.LargeContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.LargeContainerHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeColor,
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
    enabled: Boolean,
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
                enabled = enabled,
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
                enabled = enabled,
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
 * 图标选择网格。
 *
 * 使用 [FlowRow] 自适应换行，展示 [AvailableCategoryIcons] 中所有可选图标。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconSelector(
    selectedIndex: Int,
    onIconSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "选择图标",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvailableCategoryIcons.forEachIndexed { index, (iconName, iconRes) ->
                IconOption(
                    iconRes = iconRes,
                    iconName = iconName,
                    isSelected = selectedIndex == index,
                    onClick = { onIconSelected(index) }
                )
            }
        }
    }
}

/**
 * 单个图标选项。
 */
@Composable
private fun IconOption(
    iconRes: Int,
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionShape = MaterialTheme.shapes.medium
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = optionShape,
        color = containerColor,
        modifier = modifier.size(56.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MoniIcon(
                icon = iconRes,
                contentDescription = iconName,
                tint = iconTint
            )
        }
    }
}

private const val DESCRIPTION_MAX_LENGTH = 200
