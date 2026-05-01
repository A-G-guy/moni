package com.agguy.moni.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RustCoreController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val rustCore = RustCoreController()
    private val effectRunner = CoreEffectRunner()

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    init {
        effectRunner.onShowSnackbar = { message ->
            Log.d("MoniSnackbar", message)
        }
        effectRunner.onNavigate = { screen ->
            Log.d("MoniNavigate", screen)
        }
        effectRunner.onExportFile = { format, content ->
            Log.d("MoniExport", "format=$format, contentLength=${content.length}")
        }

        viewModelScope.launch {
            try {
                val dbPath = application.filesDir.absolutePath + "/moni.db"
                val mutation = rustCore.initializeWithDb(dbPath)
                applyMutation(mutation)
            } catch (e: Exception) {
                Log.e("MoniInit", "数据库初始化失败，回退到内存模式", e)
                val mutation = rustCore.initialize()
                applyMutation(mutation)
            }
        }
    }

    fun dispatch(intent: CoreIntent) {
        viewModelScope.launch {
            val mutation = rustCore.dispatch(intent)
            applyMutation(mutation)
        }
    }

    private fun applyMutation(mutation: com.agguy.moni.core.CoreMutation) {
        _uiState.value = mutation.state.toAppState()
        mutation.effects.forEach { effectRunner.runEffect(it) }
    }
}
