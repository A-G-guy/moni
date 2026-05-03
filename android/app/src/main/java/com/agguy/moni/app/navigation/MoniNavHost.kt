package com.agguy.moni.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.agguy.moni.app.AppState
import com.agguy.moni.app.ThemeSettings
import com.agguy.moni.app.ui.category.CategoryListScreen
import com.agguy.moni.app.ui.record.RecordDetailScreen
import com.agguy.moni.app.ui.record.RecordListScreen
import com.agguy.moni.app.ui.settings.SettingsScreen
import com.agguy.moni.app.ui.stats.StatsScreen
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.core.CoreIntent

@Composable
fun MoniNavHost(
    navController: NavHostController,
    appState: AppState,
    themeSettings: ThemeSettings,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateToRecordDetail: (Long?) -> Unit,
    onNavigateToCategoryList: () -> Unit,
    onNavigateBack: () -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RecordList,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        composable<Screen.RecordList> {
            RecordListScreen(
                appState = appState,
                onDispatch = onDispatch,
                onNavigateToRecordDetail = onNavigateToRecordDetail,
                onNavigateToCategoryList = onNavigateToCategoryList
            )
        }
        composable<Screen.RecordDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.RecordDetail>()
            RecordDetailScreen(
                appState = appState,
                recordId = detail.recordId,
                onDispatch = onDispatch,
                onNavigateBack = onNavigateBack
            )
        }
        composable<Screen.CategoryList> {
            CategoryListScreen(
                appState = appState,
                onDispatch = onDispatch,
                onNavigateBack = onNavigateBack
            )
        }
        composable<Screen.Stats> {
            StatsScreen(
                appState = appState,
                onDispatch = onDispatch
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                appState = appState,
                themeSettings = themeSettings,
                onDispatch = onDispatch,
                onNavigateBack = onNavigateBack,
                onUpdateThemeMode = onUpdateThemeMode,
                onUpdateDynamicColor = onUpdateDynamicColor
            )
        }
    }
}
