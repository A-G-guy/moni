package com.agguy.moni.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.agguy.moni.app.AppState
import com.agguy.moni.app.GreetingScreen
import com.agguy.moni.app.ui.category.CategoryListScreen
import com.agguy.moni.app.ui.record.RecordDetailScreen
import com.agguy.moni.app.ui.record.RecordListScreen
import com.agguy.moni.core.CoreIntent

@Composable
fun MoniNavHost(
    navController: NavHostController,
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateToRecordDetail: (Long?) -> Unit,
    onNavigateToCategoryList: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RecordList,
        modifier = modifier
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
            // 迭代三实现统计页
            GreetingScreen(
                appState = appState,
                onDispatch = onDispatch
            )
        }
        composable<Screen.Settings> {
            // 迭代三实现设置页
            GreetingScreen(
                appState = appState,
                onDispatch = onDispatch
            )
        }
    }
}
