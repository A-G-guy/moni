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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.components.SettingsItem
import com.agguy.moni.app.icons.SymbolIcon
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
                        stringResource(R.string.dev_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = "arrow_back",
                            contentDescription = stringResource(R.string.back),
                            size = 24.dp
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
                iconName = "info",
                title = stringResource(R.string.dev_copy_logs),
                subtitle = stringResource(R.string.dev_logs_subtitle),
                onClick = onNavigateToDevLog
            )

            SettingsItem(
                iconName = "add",
                title = stringResource(R.string.dev_generate_mock),
                subtitle = stringResource(R.string.dev_mock_subtitle),
                onClick = { showMockDataDialog = true }
            )

            SettingsItem(
                iconName = "delete",
                title = stringResource(R.string.dev_clear_all),
                subtitle = stringResource(R.string.dev_clear_subtitle),
                onClick = { showClearDataDialog = true }
            )

            SettingsItem(
                iconName = "category",
                title = stringResource(R.string.dev_reset_presets),
                subtitle = stringResource(R.string.dev_reset_presets_subtitle),
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
