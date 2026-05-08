@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.icons.SymbolIcon
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
fun IconPicker(selectedIconName: String, onIconSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredGroups = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            GroupedCategoryIcons
        } else {
            val query = searchQuery.lowercase()
            GroupedCategoryIcons.mapNotNull { group ->
                val matched = group.icons.filter {
                    it.name.lowercase().contains(query) || it.displayName.contains(query)
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
            label = { Text(stringResource(R.string.category_search_icon_hint)) },
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
                    text = stringResource(R.string.category_no_match),
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
 * 使用 [BoxWithConstraints] 动态计算列数，以普通 [Row]/[Column] 替代 [LazyVerticalGrid]，
 * 避免懒加载网格嵌套在可滚动容器内导致的子项渲染异常。
 *
 * @param group 图标分组数据
 * @param selectedIconName 当前选中的图标名称
 * @param onIconSelected 图标选中回调
 */
@Composable
private fun IconGroupSection(group: IconGroup, selectedIconName: String, onIconSelected: (String) -> Unit) {
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

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            val itemSize = 64.dp
            val spacing = 8.dp
            val columns = maxOf(
                1,
                ((maxWidth + spacing) / (itemSize + spacing)).toInt()
            )
            val rows = (group.icons.size + columns - 1) / columns

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (col in 0 until columns) {
                            val index = row * columns + col
                            if (index < group.icons.size) {
                                val icon = group.icons[index]
                                IconPickerOption(
                                    icon = icon,
                                    isSelected = icon.name == selectedIconName,
                                    onClick = { onIconSelected(icon.name) },
                                    modifier = Modifier.size(itemSize)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(itemSize))
                            }
                        }
                    }
                }
            }
        }
    }
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
    Surface(
        onClick = onClick,
        shape = optionShape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = modifier.clip(optionShape),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            SymbolIcon(
                name = icon.name,
                filled = isSelected,
                contentDescription = icon.displayName,
                size = 32.dp,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = icon.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}
