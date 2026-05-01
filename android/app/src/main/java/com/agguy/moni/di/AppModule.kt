package com.agguy.moni.di

import android.content.Context
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.RustCoreController

/**
 * 手动依赖注入模块。
 *
 * MVP 阶段采用工厂模式管理核心组件的生命周期，
 * 未来可平滑迁移至 Hilt/Koin 等正式 DI 框架。
 */
object AppModule {
    fun provideRustCoreController(): RustCoreController = RustCoreController()

    fun provideCoreEffectRunner(context: Context): CoreEffectRunner {
        return CoreEffectRunner().apply {
            onExportFile = { format, content ->
                com.agguy.moni.core.platform.ExportHelper.saveToDownloads(
                    context, format, content
                )
            }
        }
    }
}
