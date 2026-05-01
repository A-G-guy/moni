package com.agguy.moni.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.RustCoreController

/**
 * AppViewModel 的自定义工厂。
 *
 * 支持通过依赖注入传递核心组件实例。
 */
class AppViewModelFactory(
    private val application: Application,
    private val rustCore: RustCoreController,
    private val effectRunner: CoreEffectRunner,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(application, rustCore, effectRunner) as T
        }
        throw IllegalArgumentException("未知的 ViewModel 类型: ${modelClass.name}")
    }
}
