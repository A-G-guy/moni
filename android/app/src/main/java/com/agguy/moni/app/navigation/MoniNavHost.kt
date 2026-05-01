package com.agguy.moni.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agguy.moni.app.GreetingScreen
import com.agguy.moni.app.AppState
import com.agguy.moni.core.CoreIntent

@Composable
fun MoniNavHost(
    navController: NavHostController,
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RecordList,
        modifier = modifier
    ) {
        composable<Screen.RecordList> {
            // 迭代二实现记账列表页
            GreetingScreen(
                appState = appState,
                onDispatch = onDispatch
            )
        }
        composable<Screen.RecordDetail> {
            // 迭代二实现记账详情/表单页
        }
        composable<Screen.CategoryList> {
            // 迭代二实现分类管理页
        }
        composable<Screen.Stats> {
            // 迭代三实现统计页
        }
        composable<Screen.Settings> {
            // 迭代三实现设置页
        }
    }
}
