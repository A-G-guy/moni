@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.agguy.moni.R
import com.agguy.moni.app.NumPadSettings
import com.agguy.moni.app.RecordItemDisplaySettings
import com.agguy.moni.app.ThemeSettings
import com.agguy.moni.app.components.SettingsToggleItem
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.app.theme.displayName
import com.agguy.moni.app.theme.primaryColor
import com.agguy.moni.app.theme.seedColor

/**
 * 外观设置二级页面。
 *
 * 整合配色方案、主题模式两个设置项，页面内直接展示选择器，选择即时生效。
 *
 * 返回导航层级（由内到外）：
 * 1. 页面级：子页面 — popBackStack 返回
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    themeSettings: ThemeSettings,
    recordItemDisplaySettings: RecordItemDisplaySettings,
    numPadSettings: NumPadSettings = NumPadSettings(),
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdatePresetColorScheme: (PresetColorScheme) -> Unit,
    onUpdateRecordShowIcon: (Boolean) -> Unit,
    onUpdateRecordShowFullCategory: (Boolean) -> Unit,
    onUpdateRecordNotePriority: (Boolean) -> Unit,
    onUpdateNumPadSwapTopAndBottomRows: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.theme_settings_title),
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // === 配色方案 ===
            SectionTitle(stringResource(R.string.theme_color_scheme))
            ColorSchemeSelector(
                currentScheme = themeSettings.presetColorScheme,
                onSchemeSelected = onUpdatePresetColorScheme
            )

            HorizontalDivider()

            // === 主题模式 ===
            SectionTitle(stringResource(R.string.theme_mode))
            ThemeModeSelector(
                currentMode = themeSettings.themeMode,
                onModeSelected = onUpdateThemeMode
            )

            HorizontalDivider()

            // === 账单条目内容 ===
            SectionTitle(stringResource(R.string.theme_record_item_content))
            SettingsToggleItem(
                iconName = "category",
                title = stringResource(R.string.theme_show_icon),
                subtitle = stringResource(R.string.theme_show_icon_subtitle),
                checked = recordItemDisplaySettings.showIcon,
                onCheckedChange = onUpdateRecordShowIcon
            )
            SettingsToggleItem(
                iconName = "filter_list",
                title = stringResource(R.string.theme_show_full_category),
                subtitle = stringResource(R.string.theme_show_full_category_subtitle),
                checked = recordItemDisplaySettings.showFullCategory,
                onCheckedChange = onUpdateRecordShowFullCategory
            )
            SettingsToggleItem(
                iconName = "edit",
                title = stringResource(R.string.theme_note_priority),
                subtitle = stringResource(R.string.theme_note_priority_subtitle),
                checked = recordItemDisplaySettings.notePriority,
                onCheckedChange = onUpdateRecordNotePriority
            )

            HorizontalDivider()

            // === 小键盘 ===
            SectionTitle(stringResource(R.string.theme_numpad_settings))
            SettingsToggleItem(
                iconName = "numbers",
                title = stringResource(R.string.theme_numpad_swap_top_bottom),
                subtitle = stringResource(R.string.theme_numpad_swap_top_bottom_subtitle),
                checked = numPadSettings.swapTopAndBottomRows,
                onCheckedChange = onUpdateNumPadSwapTopAndBottomRows
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * 预设配色方案选择器。
 *
 * 水平滚动的圆形色块网格，每个色块展示该配色的种子色，
 * 选中态用 M3 生成主色作为边框高亮。
 */
@Composable
private fun ColorSchemeSelector(
    currentScheme: PresetColorScheme,
    onSchemeSelected: (PresetColorScheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val schemes = PresetColorScheme.entries

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(schemes) { scheme ->
            val selected = scheme == currentScheme
            val border = if (selected) {
                BorderStroke(2.dp, scheme.primaryColor)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { onSchemeSelected(scheme) },
                    shape = CircleShape,
                    color = scheme.seedColor,
                    border = border,
                    modifier = Modifier.size(48.dp)
                ) {}

                Text(
                    text = scheme.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
        ThemeMode.SYSTEM to stringResource(R.string.theme_system)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = currentMode == mode
            val containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val border = if (selected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onModeSelected(mode) },
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                contentColor = contentColor,
                border = border
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
