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
    // RustCoreController 必须全局单例：它内部持有 MoniCore（含 SQLite 连接），
    // 多处创建独立实例会导致 BackupViewModel 使用未初始化的空内存数据库。
    private val rustCoreController: RustCoreController by lazy { RustCoreController() }

    fun provideRustCoreController(): RustCoreController = rustCoreController

    fun provideCoreEffectRunner(context: Context): CoreEffectRunner = CoreEffectRunner()
}
