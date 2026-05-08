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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.BuildConfig
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.ThemeSettings
import com.agguy.moni.app.components.SettingsItem
import com.agguy.moni.app.i18n.AppLocaleManager
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.app.theme.displayName
import com.agguy.moni.core.CoreIntent

/**
 * 设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appState: AppState,
    themeSettings: ThemeSettings,
    language: AppLocaleManager.AppLanguage,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToDeveloperOptions: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onUpdateLanguage: (AppLocaleManager.AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val context = androidx.compose.ui.platform.LocalContext.current
    val themeModeLabel = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }

    val appearanceSubtitle = "$themeModeLabel · ${themeSettings.presetColorScheme.displayName()}"

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
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
                iconName = "palette",
                title = stringResource(R.string.settings_appearance),
                subtitle = appearanceSubtitle,
                onClick = onNavigateToThemeSettings
            )

            SettingsItem(
                iconName = "translate",
                title = stringResource(R.string.settings_language),
                subtitle = when (language) {
                    AppLocaleManager.AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                    AppLocaleManager.AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                    AppLocaleManager.AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                },
                onClick = { showLanguageDialog = true }
            )

            SettingsItem(
                iconName = "attach_money",
                title = stringResource(R.string.settings_currency),
                subtitle = stringResource(R.string.settings_currency_current, appState.currencySymbol),
                onClick = { showCurrencyDialog = true }
            )

            SettingsItem(
                iconName = "cloud",
                title = stringResource(R.string.settings_data_management),
                subtitle = stringResource(R.string.settings_data_management_subtitle),
                onClick = onNavigateToDataManagement
            )

            SettingsItem(
                iconName = "tune",
                title = stringResource(R.string.settings_developer),
                subtitle = stringResource(R.string.settings_developer_subtitle),
                onClick = onNavigateToDeveloperOptions
            )

            SettingsItem(
                iconName = "help",
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
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
        visible = showLanguageDialog,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        if (showLanguageDialog) {
            LanguagePickerDialog(
                currentLanguage = language,
                onConfirm = { selected ->
                    if (selected != language) {
                        onUpdateLanguage(selected)
                    }
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}
