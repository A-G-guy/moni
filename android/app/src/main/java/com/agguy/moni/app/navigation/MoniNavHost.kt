@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.agguy.moni.app.AppState
import com.agguy.moni.app.RecordItemDisplaySettings
import com.agguy.moni.app.SearchParams
import com.agguy.moni.app.ThemeSettings
import com.agguy.moni.app.i18n.AppLocaleManager
import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.app.ui.backup.BackupViewModel
import com.agguy.moni.app.ui.backup.DataManagementScreen
import com.agguy.moni.app.ui.category.ArchivedCategoriesScreen
import com.agguy.moni.app.ui.category.CategoryListScreen
import com.agguy.moni.app.ui.dev.DevLogScreen
import com.agguy.moni.app.ui.dev.DeveloperOptionsScreen
import com.agguy.moni.app.ui.record.RecordDetailScreen
import com.agguy.moni.app.ui.record.RecordListScreen
import com.agguy.moni.app.ui.aibookkeeping.AiBookkeepingScreen
import com.agguy.moni.app.ui.aibookkeeping.AiBookkeepingViewModel
import com.agguy.moni.app.ui.settings.SettingsScreen
import com.agguy.moni.app.ui.stats.StatsScreen
import com.agguy.moni.app.repository.ChatRepositoryImpl
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RustCoreController

/**
 * 底部栏页面的左右顺序索引，用于决定切换时动画方向。
 * 顺序：账单 < 统计 < 设置。
 */
private fun NavDestination?.bottomTabIndex(): Int? = when {
    this == null -> null
    hasRoute(Screen.RecordList::class) -> 0
    hasRoute(Screen.Stats::class) -> 1
    hasRoute(Screen.Settings::class) -> 2
    else -> null
}

/**
 * 根据起止 destination 决定切换方向：
 * - 都是底部栏页面时，按 tab 索引大小决定 Start/End；
 * - 否则（详情页等）回退到默认方向。
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.resolveSlideDirection(
    fallback: SlideDirection
): SlideDirection {
    val from = initialState.destination.bottomTabIndex()
    val to = targetState.destination.bottomTabIndex()
    return if (from != null && to != null && from != to) {
        if (to > from) SlideDirection.Start else SlideDirection.End
    } else {
        fallback
    }
}

/**
 * 应用主导航宿主。
 *
 * Material 3 Expressive 改造点：移除写死的 [androidx.compose.animation.core.tween] 时长，
 * 改为读取 [MaterialTheme.motionScheme] 提供的 default spatial/effects spec，让页面切换的曲线
 * 与全局 motion 主题保持一致，未来可通过切换 [androidx.compose.material3.MotionScheme] 整体调速。
 */
@Composable
fun MoniNavHost(
    navController: NavHostController,
    appState: AppState,
    themeSettings: ThemeSettings,
    recordItemDisplaySettings: RecordItemDisplaySettings,
    selectedYearMonth: String,
    language: AppLocaleManager.AppLanguage = AppLocaleManager.AppLanguage.SYSTEM,
    onDispatch: (CoreIntent) -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onNavigateToRecordDetail: (Long?) -> Unit,
    onNavigateToCategoryList: () -> Unit,
    onNavigateToArchivedCategories: () -> Unit,
    onNavigateBack: () -> Unit,
    onCheckBudget: (Long, Long) -> Unit = { _, _ -> },
    onClearBudgetCheck: () -> Unit = {},
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdatePresetColorScheme: (PresetColorScheme) -> Unit,
    onUpdateRecordShowIcon: (Boolean) -> Unit = {},
    onUpdateRecordShowFullCategory: (Boolean) -> Unit = {},
    onUpdateRecordNotePriority: (Boolean) -> Unit = {},
    onUpdateLanguage: (AppLocaleManager.AppLanguage) -> Unit = {},
    onNavigateToDeveloperOptions: () -> Unit = {},
    onNavigateToDevLog: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToBudgetList: () -> Unit = {},
    onNavigateToAiBookkeeping: () -> Unit = {},
    onCreateAiRecord: suspend (CoreIntent.RecordCreate) -> Boolean = { false },
    onEnterSearchMode: () -> Unit = {},
    onExitSearchMode: () -> Unit = {},
    onUpdateSearchKeyword: (String) -> Unit = {},
    onUpdateSearchParams: (com.agguy.moni.app.SearchParams) -> Unit = {},
    onResetSearchParams: () -> Unit = {},
    backupViewModel: BackupViewModel,
    dbPath: String,
    rustCore: RustCoreController,
    modifier: Modifier = Modifier
) {
    val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    NavHost(
        navController = navController,
        startDestination = Screen.RecordList,
        modifier = modifier,
        enterTransition = {
            val direction = resolveSlideDirection(SlideDirection.Start)
            fadeIn(animationSpec = fadeSpec) +
                slideIntoContainer(towards = direction, animationSpec = slideSpec)
        },
        exitTransition = {
            val direction = resolveSlideDirection(SlideDirection.Start)
            fadeOut(animationSpec = fadeSpec) +
                slideOutOfContainer(towards = direction, animationSpec = slideSpec)
        },
        popEnterTransition = {
            val direction = resolveSlideDirection(SlideDirection.End)
            fadeIn(animationSpec = fadeSpec) +
                slideIntoContainer(towards = direction, animationSpec = slideSpec)
        },
        popExitTransition = {
            val direction = resolveSlideDirection(SlideDirection.End)
            fadeOut(animationSpec = fadeSpec) +
                slideOutOfContainer(towards = direction, animationSpec = slideSpec)
        }
    ) {
        composable<Screen.RecordList> {
            RecordListScreen(
                appState = appState,
                selectedYearMonth = selectedYearMonth,
                recordItemDisplaySettings = recordItemDisplaySettings,
                onDispatch = onDispatch,
                onSelectYearMonth = onSelectYearMonth,
                onNavigateToRecordDetail = onNavigateToRecordDetail,
                onNavigateToCategoryList = onNavigateToCategoryList,
                onNavigateToBudgetList = onNavigateToBudgetList,
                onNavigateToAiBookkeeping = onNavigateToAiBookkeeping,
                onEnterSearchMode = onEnterSearchMode,
                onExitSearchMode = onExitSearchMode,
                onUpdateSearchKeyword = onUpdateSearchKeyword,
                onUpdateSearchParams = onUpdateSearchParams,
                onResetSearchParams = onResetSearchParams
            )
        }
        composable<Screen.RecordDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.RecordDetail>()
            RecordDetailScreen(
                appState = appState,
                recordId = detail.recordId,
                onDispatch = onDispatch,
                onNavigateBack = onNavigateBack,
                onCheckBudget = onCheckBudget,
                onClearBudgetCheck = onClearBudgetCheck
            )
        }
        composable<Screen.CategoryList> {
            CategoryListScreen(
                appState = appState,
                onDispatch = onDispatch,
                onNavigateBack = onNavigateBack,
                onNavigateToArchivedCategories = onNavigateToArchivedCategories
            )
        }
        composable<Screen.ArchivedCategories> {
            ArchivedCategoriesScreen(
                appState = appState,
                onDispatch = onDispatch,
                onNavigateBack = onNavigateBack
            )
        }
        composable<Screen.Stats> {
            StatsScreen(
                appState = appState,
                selectedYearMonth = selectedYearMonth,
                onDispatch = onDispatch,
                onSelectYearMonth = onSelectYearMonth,
                onNavigateToBudget = onNavigateToBudgetList
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                appState = appState,
                themeSettings = themeSettings,
                language = language,
                onDispatch = onDispatch,
                onNavigateToThemeSettings = onNavigateToThemeSettings,
                onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                onNavigateToDataManagement = onNavigateToDataManagement,
                onUpdateLanguage = onUpdateLanguage,
            )
        }
        composable<Screen.DataManagement> {
            DataManagementScreen(
                viewModel = backupViewModel,
                dbPath = dbPath,
                onNavigateBack = onNavigateBack,
            )
        }
        composable<Screen.DeveloperOptions> {
            DeveloperOptionsScreen(
                onNavigateToDevLog = onNavigateToDevLog,
                onNavigateBack = onNavigateBack,
                onDispatch = onDispatch,
                onClearAllData = onClearAllData
            )
        }
        composable<Screen.DevLog> {
            DevLogScreen(
                onNavigateBack = onNavigateBack
            )
        }
        composable<Screen.ThemeSettings> {
            com.agguy.moni.app.ui.settings.ThemeSettingsScreen(
                themeSettings = themeSettings,
                recordItemDisplaySettings = recordItemDisplaySettings,
                onUpdateThemeMode = onUpdateThemeMode,
                onUpdatePresetColorScheme = onUpdatePresetColorScheme,
                onUpdateRecordShowIcon = onUpdateRecordShowIcon,
                onUpdateRecordShowFullCategory = onUpdateRecordShowFullCategory,
                onUpdateRecordNotePriority = onUpdateRecordNotePriority,
                onNavigateBack = onNavigateBack
            )
        }
        composable<Screen.BudgetList> {
            com.agguy.moni.app.ui.budget.BudgetListScreen(
                appState = appState,
                selectedYearMonth = selectedYearMonth,
                onDispatch = onDispatch,
                onSelectYearMonth = onSelectYearMonth,
                onNavigateBack = onNavigateBack
            )
        }
        composable<Screen.AiBookkeeping> {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<AiBookkeepingViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return AiBookkeepingViewModel(
                            chatRepository = ChatRepositoryImpl(rustCore.core),
                            onCreateRecord = onCreateAiRecord
                        ) as T
                    }
                }
            )
            AiBookkeepingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecordList = { navController.navigate(Screen.RecordList) }
            )
        }
    }
}
