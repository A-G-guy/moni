package com.agguy.moni.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RustCoreController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val rustCore = RustCoreController()
    private val _effectRunner = CoreEffectRunner()

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private var _navController: NavHostController? = null

    /** 供 UI 层绑定导航控制器。 */
    fun bindNavController(navController: NavHostController) {
        _navController = navController
    }

    /** 供 UI 层绑定 Snackbar 回调。 */
    fun bindSnackbarCallback(callback: (String) -> Unit) {
        _effectRunner.onShowSnackbar = callback
    }

    /** 供 UI 层绑定导出文件回调。 */
    fun bindExportCallback(callback: (String, String) -> Unit) {
        _effectRunner.onExportFile = callback
    }

    init {
        _effectRunner.onNavigate = { screen ->
            Log.d("MoniNavigate", screen)
        }
        _effectRunner.onExportFile = { format, content ->
            com.agguy.moni.core.platform.ExportHelper.saveToDownloads(
                getApplication(), format, content
            )
        }

        viewModelScope.launch {
            try {
                val dbPath = application.filesDir.absolutePath + "/moni.db"
                val mutation = rustCore.initializeWithDb(dbPath)
                applyMutation(mutation)
                // 加载初始数据
                dispatch(CoreIntent.CategoryList)
                dispatch(CoreIntent.RecordList(page = 0, pageSize = 50))
                // 同步 DataStore 中保存的货币符号
                syncCurrencySymbolFromDataStore()
            } catch (e: Exception) {
                Log.e("MoniInit", "数据库初始化失败，回退到内存模式", e)
                val mutation = rustCore.initialize()
                applyMutation(mutation)
            }
        }
    }

    fun dispatch(intent: CoreIntent) {
        viewModelScope.launch {
            if (intent is CoreIntent.SettingsUpdateCurrency) {
                com.agguy.moni.core.platform.DataStoreHelper.saveCurrencySymbol(
                    getApplication(), intent.symbol
                )
            }
            val mutation = rustCore.dispatch(intent)
            applyMutation(mutation)
        }
    }

    private suspend fun syncCurrencySymbolFromDataStore() {
        val savedSymbol = com.agguy.moni.core.platform.DataStoreHelper
            .currencySymbolFlow(getApplication())
            .first()
        if (savedSymbol != _uiState.value.currencySymbol) {
            dispatch(CoreIntent.SettingsUpdateCurrency(symbol = savedSymbol))
        }
    }

    fun navigateToRecordDetail(recordId: Long? = null) {
        _navController?.navigate(Screen.RecordDetail(recordId))
    }

    fun navigateToCategoryList() {
        _navController?.navigate(Screen.CategoryList)
    }

    fun navigateBack() {
        _navController?.popBackStack()
    }

    private fun applyMutation(mutation: com.agguy.moni.core.CoreMutation) {
        _uiState.value = mutation.state.toAppState()
        mutation.effects.forEach { _effectRunner.runEffect(it) }
    }
}
