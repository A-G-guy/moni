@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 *
 * 提供货币符号设置、主题模式、主题色、动态颜色、数据导出、关于应用等功能入口。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    appState: AppState,
    themeSettings: ThemeSettings,
    onDispatch: (CoreIntent) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdateSeedColor: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showSeedColorDialog by remember { mutableStateOf(false) }

    val themeModeLabel = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
        ThemeMode.SYSTEM -> "跟随系统"
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.displaySmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
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
                icon = MoniIcons.ArrowBack,
                title = "导出数据",
                subtitle = "导出为 CSV 或 JSON 格式",
                onClick = { showExportDialog = true }
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
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
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
        visible = showExportDialog,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
    ) {
        if (showExportDialog) {
            ExportDataDialog(
                onConfirm = { format ->
                    onDispatch(CoreIntent.SettingsExportData(format = format))
                    showExportDialog = false
                },
                onDismiss = { showExportDialog = false }
            )
        }
    }

    AnimatedVisibility(
        visible = showThemeModeDialog,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
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
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
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
