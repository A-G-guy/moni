package com.agguy.moni.app.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory

/**
 * 父分类选择器 BottomSheet。
 *
 * 展示同类型的一级分类列表，供用户选择作为当前分类的父分类。
 *
 * @param parents 可选的一级分类列表
 * @param selectedParentId 当前选中的父分类 ID
 * @param onParentSelected 选择回调，传 null 表示不选父分类
 * @param onDismiss 关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentCategoryPickerSheet(
    parents: List<CoreCategory>,
    selectedParentId: Long?,
    onParentSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
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
        ) {
            Text(
                text = "选择父分类",
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // "不作为子分类"选项
            Surface(
                onClick = {
                    onParentSelected(null)
                    onDismiss()
                },
                shape = MaterialTheme.shapes.medium,
                color = if (selectedParentId == null)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "作为一级分类（不使用父分类）",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedParentId == null)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parents) { parent ->
                    val isSelected = parent.id == selectedParentId
                    val parentColor = if (parent.categoryType == "expense")
                        MaterialTheme.colorScheme.expenseRed
                    else
                        MaterialTheme.colorScheme.incomeGreen

                    Surface(
                        onClick = {
                            onParentSelected(parent.id)
                            onDismiss()
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(parentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                MoniIcon(
                                    icon = iconNameToRes(parent.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = parentColor
                                )
                            }

                            Text(
                                text = parent.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                MoniIcon(
                                    icon = MoniIcons.Check,
                                    contentDescription = "已选中",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
