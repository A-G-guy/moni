@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R

/**
 * 图标选择器 BottomSheet。
 *
 * 包装 [IconPicker] 组件，提供搜索和分组展示能力。
 * 点击图标即选中，底部提供「确认」按钮关闭 Sheet。
 *
 * @param selectedIconName 当前选中的图标名称
 * @param onIconSelected 图标选中回调
 * @param onDismiss 关闭 Sheet 回调
 */
@Composable
fun IconPickerSheet(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.category_select_icon),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 图标选择器（限制最大高度避免 BottomSheet 溢出）
            IconPicker(
                selectedIconName = selectedIconName,
                onIconSelected = onIconSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            )

            // 确认按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.category_confirm),
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}
