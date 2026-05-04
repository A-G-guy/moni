package com.agguy.moni.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.app.theme.DefaultSeedColor
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RustCoreController
import com.agguy.moni.core.platform.DataStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 主题设置状态。
 */
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val seedColor: Long = DefaultSeedColor.value.toLong()
)

class AppViewModel(
    application: Application,
    private val rustCore: RustCoreController,
    private val effectRunner: CoreEffectRunner,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    private var _navController: NavHostController? = null

    /** 供 UI 层绑定导航控制器。 */
    fun bindNavController(navController: NavHostController) {
        _navController = navController
    }

    /** 供 UI 层绑定 Snackbar 回调。 */
    fun bindSnackbarCallback(callback: (String) -> Unit) {
        effectRunner.onShowSnackbar = callback
    }

    /** 供 UI 层绑定导出文件回调。 */
    fun bindExportCallback(callback: (String, String) -> Unit) {
        effectRunner.onExportFile = callback
    }

    init {
        effectRunner.onNavigate = { screen ->
            Log.d("MoniNavigate", screen)
        }

        viewModelScope.launch {
            try {
                val dbPath = application.filesDir.absolutePath + "/moni.db"
                val mutation = rustCore.initializeWithDb(dbPath)
                applyMutation(mutation)
                dispatch(CoreIntent.CategoryList)
                dispatch(CoreIntent.RecordList(page = 0, pageSize = 50))
                syncCurrencySymbolFromDataStore()
                syncThemeSettingsFromDataStore()
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
                DataStoreHelper.saveCurrencySymbol(
                    getApplication(), intent.symbol
                )
            }
            val mutation = rustCore.dispatch(intent)
            applyMutation(mutation)
        }
    }

    private suspend fun syncCurrencySymbolFromDataStore() {
        val savedSymbol = DataStoreHelper
            .currencySymbolFlow(getApplication())
            .first()
        if (savedSymbol != _uiState.value.currencySymbol) {
            dispatch(CoreIntent.SettingsUpdateCurrency(symbol = savedSymbol))
        }
    }

    private suspend fun syncThemeSettingsFromDataStore() {
        val themeMode = DataStoreHelper.themeModeFlow(getApplication()).first()
        val dynamicColor = DataStoreHelper.dynamicColorFlow(getApplication()).first()
        val seedColor = DataStoreHelper.seedColorFlow(getApplication()).first()
        _themeSettings.value = ThemeSettings(themeMode, dynamicColor, seedColor)
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            DataStoreHelper.saveThemeMode(getApplication(), mode)
            _themeSettings.value = _themeSettings.value.copy(themeMode = mode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            DataStoreHelper.saveDynamicColor(getApplication(), enabled)
            _themeSettings.value = _themeSettings.value.copy(dynamicColor = enabled)
        }
    }

    fun updateSeedColor(colorValue: Long) {
        viewModelScope.launch {
            DataStoreHelper.saveSeedColor(getApplication(), colorValue)
            _themeSettings.value = _themeSettings.value.copy(seedColor = colorValue)
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
        mutation.effects.forEach { effectRunner.runEffect(it) }
    }
}
