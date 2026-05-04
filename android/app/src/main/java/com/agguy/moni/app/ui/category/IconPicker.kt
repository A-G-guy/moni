@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.theme.CategoryIcon
import com.agguy.moni.app.theme.GroupedCategoryIcons
import com.agguy.moni.app.theme.IconGroup

/**
 * 图标选择器。
 *
 * 顶部搜索框实时过滤，下方按分组展示图标网格。
 * 选中态使用高亮容器 + 主色边框，与 Material 3 Expressive 风格一致。
 *
 * @param selectedIconName 当前选中的图标名称
 * @param onIconSelected 图标选中回调
 * @param modifier 修饰符
 */
@Composable
fun IconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredGroups = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            GroupedCategoryIcons
        } else {
            val query = searchQuery.lowercase()
            GroupedCategoryIcons.mapNotNull { group ->
                val matched = group.icons.filter {
                    it.name.lowercase().contains(query)
                }
                if (matched.isNotEmpty()) {
                    IconGroup(label = group.label, icons = matched)
                } else {
                    null
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜索图标") },
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未找到匹配的图标",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filteredGroups.forEach { group ->
                    IconGroupSection(
                        group = group,
                        selectedIconName = selectedIconName,
                        onIconSelected = onIconSelected
                    )
                }
            }
        }
    }
}

/**
 * 单个图标分组区域。
 *
 * @param group 图标分组数据
 * @param selectedIconName 当前选中的图标名称
 * @param onIconSelected 图标选中回调
 */
@Composable
private fun IconGroupSection(
    group: IconGroup,
    selectedIconName: String,
    onIconSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(calculateGridHeight(group.icons.size)),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(group.icons, key = { it.name }) { icon ->
                IconPickerOption(
                    icon = icon,
                    isSelected = icon.name == selectedIconName,
                    onClick = { onIconSelected(icon.name) }
                )
            }
        }
    }
}

/**
 * 根据图标数量计算网格高度。
 *
 * 每行约 48dp 图标 + 8dp 间距，预留上下边距。
 */
private fun calculateGridHeight(iconCount: Int): Dp {
    val columns = 6
    val rows = (iconCount + columns - 1) / columns
    val itemHeight = 48
    val spacing = 8
    val padding = 16
    return ((rows * itemHeight) + ((rows - 1) * spacing) + padding).dp
}

/**
 * 图标选择器中的单个图标选项。
 *
 * @param icon 图标数据
 * @param isSelected 是否被选中
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
private fun IconPickerOption(
    icon: CategoryIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionShape = MaterialTheme.shapes.medium
    val iconRes = if (isSelected) icon.iconResFilled else icon.iconRes

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(optionShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            shape = optionShape,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
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
                    contentDescription = icon.name,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
