@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.SettingsItem
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.core.CoreIntent

/**
 * 开发者选项页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsScreen(
    onNavigateToDevLog: () -> Unit,
    onNavigateBack: () -> Unit,
    onDispatch: (CoreIntent) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMockDataDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "开发者选项",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = MoniIcons.ArrowBack
                            ),
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsItem(
                icon = MoniIcons.Info,
                title = "复制日志",
                subtitle = "查看并导出最近日志",
                onClick = onNavigateToDevLog
            )

            SettingsItem(
                icon = MoniIcons.Add,
                title = "生成测试数据",
                subtitle = "注入 Mock 数据用于测试",
                onClick = { showMockDataDialog = true }
            )

            SettingsItem(
                icon = MoniIcons.Delete,
                title = "清空所有数据",
                subtitle = "删除所有数据并重启（危险）",
                onClick = { showClearDataDialog = true }
            )

            SettingsItem(
                icon = MoniIcons.Category,
                title = "重置预设分类",
                subtitle = "插入默认的分类模板",
                onClick = { onDispatch(CoreIntent.DevSeedPresets) }
            )
        }
    }

    if (showMockDataDialog) {
        DevMockDataDialog(
            onConfirm = { count, preset ->
                onDispatch(CoreIntent.DevGenerateMockData(count = count, preset = preset))
                showMockDataDialog = false
            },
            onDismiss = { showMockDataDialog = false }
        )
    }

    if (showClearDataDialog) {
        DevClearDataDialog(
            onConfirm = {
                showClearDataDialog = false
                onClearAllData()
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}
