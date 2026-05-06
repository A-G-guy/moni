@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.agguy.moni.BuildConfig
import com.agguy.moni.app.AppState
import com.agguy.moni.app.ThemeSettings
import com.agguy.moni.app.components.SettingsItem
import com.agguy.moni.app.components.SettingsToggleItem
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.core.CoreIntent

/**
 * 设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appState: AppState,
    themeSettings: ThemeSettings,
    onDispatch: (CoreIntent) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdateSeedColor: (Long) -> Unit = {},
    onNavigateToDeveloperOptions: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showSeedColorDialog by remember { mutableStateOf(false) }

    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val themeModeLabel = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
        ThemeMode.SYSTEM -> "跟随系统"
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
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
                icon = MoniIcons.DarkMode,
                title = "主题模式",
                subtitle = "当前: $themeModeLabel",
                onClick = { showThemeModeDialog = true }
            )

            SettingsItem(
                icon = MoniIcons.Palette,
                title = "主题色",
                subtitle = "选择应用主色调",
                onClick = { showSeedColorDialog = true }
            )

            SettingsToggleItem(
                icon = MoniIcons.Tune,
                title = "动态颜色",
                subtitle = "使用系统壁纸颜色（Android 12+）",
                checked = themeSettings.dynamicColor,
                onCheckedChange = { onUpdateDynamicColor(it) }
            )

            SettingsItem(
                icon = MoniIcons.AttachMoney,
                title = "货币符号",
                subtitle = "当前: ${appState.currencySymbol}",
                onClick = { showCurrencyDialog = true }
            )

            SettingsItem(
                icon = MoniIcons.Cloud,
                title = "数据管理",
                subtitle = "备份导出、导入恢复、管理应用内备份",
                onClick = onNavigateToDataManagement
            )

            SettingsItem(
                icon = MoniIcons.Tune,
                title = "开发者选项",
                subtitle = "日志、Mock 数据、清空数据",
                onClick = onNavigateToDeveloperOptions
            )

            SettingsItem(
                icon = MoniIcons.Help,
                title = "关于",
                subtitle = "Moni v${BuildConfig.VERSION_NAME}",
                onClick = { }
            )
        }
    }

    AnimatedVisibility(
        visible = showCurrencyDialog,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        if (showCurrencyDialog) {
            CurrencyPickerDialog(
                currentSymbol = appState.currencySymbol,
                onConfirm = { symbol ->
                    onDispatch(CoreIntent.SettingsUpdateCurrency(symbol = symbol))
                    showCurrencyDialog = false
                },
                onDismiss = { showCurrencyDialog = false }
            )
        }
    }

    AnimatedVisibility(
        visible = showThemeModeDialog,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        if (showThemeModeDialog) {
            ThemeModePickerDialog(
                currentMode = themeSettings.themeMode,
                onConfirm = { mode ->
                    onUpdateThemeMode(mode)
                    showThemeModeDialog = false
                },
                onDismiss = { showThemeModeDialog = false }
            )
        }
    }

    AnimatedVisibility(
        visible = showSeedColorDialog,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        if (showSeedColorDialog) {
            SeedColorPickerDialog(
                currentSeed = themeSettings.seedColor,
                onConfirm = { seedColor ->
                    onUpdateSeedColor(seedColor)
                    showSeedColorDialog = false
                },
                onDismiss = { showSeedColorDialog = false }
            )
        }
    }
}
