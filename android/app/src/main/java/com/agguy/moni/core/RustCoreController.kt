package com.agguy.moni.core

import com.agguy.moni.core.platform.LogCollector
import kotlinx.serialization.encodeToString
import uniffi.moni_core.MoniCore

data class CoreMutation(val state: CoreAppState, val effects: List<CoreEffect>)

class RustCoreController {
    private val core = MoniCore()

    suspend fun initialize(): CoreMutation {
        val update = core.initialize()
        return decodeMutation(update)
    }

    suspend fun initializeWithDb(dbPath: String): CoreMutation {
        val update = core.initializeWithDb(dbPath)
        return decodeMutation(update)
    }

    suspend fun dispatch(intent: CoreIntent): CoreMutation {
        val update = core.dispatch(BridgeJsonEncode.encodeToString(intent))
        return decodeMutation(update)
    }

    suspend fun backupExport(
        outZipPath: String,
        settingsJson: String,
        appVersionName: String,
        appVersionCode: Long,
        deviceManufacturer: String,
        deviceModel: String,
        androidSdk: Int,
        progress: uniffi.moni_core.BackupProgressListener? = null,
    ): uniffi.moni_core.BackupExportReport {
        return core.backupExport(
            outZipPath,
            settingsJson,
            appVersionName,
            appVersionCode,
            deviceManufacturer,
            deviceModel,
            androidSdk,
            progress,
        )
    }

    suspend fun backupInspect(inZipPath: String): uniffi.moni_core.BackupInspection {
        return core.backupInspect(inZipPath)
    }

    suspend fun backupRestore(
        inZipPath: String,
        dbPath: String,
        progress: uniffi.moni_core.BackupProgressListener? = null,
    ): uniffi.moni_core.BackupRestoreReport {
        return core.backupRestore(inZipPath, dbPath, progress)
    }

    suspend fun autoBackupShouldRun(
        lastBackupTime: String?,
        frequency: String,
    ): Boolean {
        return core.autoBackupShouldRun(lastBackupTime, frequency)
    }

    suspend fun autoBackupPerform(
        backupDir: String,
        settingsJson: String,
        appVersionName: String,
        appVersionCode: Long,
        deviceManufacturer: String,
        deviceModel: String,
        androidSdk: Int,
        progress: uniffi.moni_core.BackupProgressListener? = null,
    ): uniffi.moni_core.AutoBackupReport {
        return core.autoBackupPerform(
            backupDir,
            settingsJson,
            appVersionName,
            appVersionCode,
            deviceManufacturer,
            deviceModel,
            androidSdk,
            progress,
        )
    }

    suspend fun autoBackupCleanup(
        backupDir: String,
        maxCount: UInt,
    ): UInt {
        return core.autoBackupCleanup(backupDir, maxCount)
    }

    // === 纯计算函数（同步调用，不涉及数据库） ===

    /** 解析表达式并返回计算结果（分）。 */
    fun evaluateExpression(expression: String): Long? {
        return core.evaluateExpression(expression)
    }

    /** 判断表达式是否包含未计算的运算符。 */
    fun hasPendingOperation(expression: String): Boolean {
        return core.hasPendingOperation(expression)
    }

    /** 格式化表达式用于显示，在运算符两侧添加空格。 */
    fun formatExpressionForDisplay(expression: String): String {
        return core.formatExpressionForDisplay(expression)
    }

    private fun decodeMutation(update: uniffi.moni_core.CoreUpdate): CoreMutation {
        return try {
            CoreMutation(
                state = BridgeJson.decodeFromString(CoreAppState.serializer(), update.stateJson),
                effects = update.effects.map { CoreEffect(it.kind, it.payloadJson) }
            )
        } catch (e: Exception) {
            val err = "JSON 反序列化失败: ${e.javaClass.simpleName}: ${e.message?.take(80)}"
            LogCollector.e("RustCoreController", err)
            // 将错误信息和部分原始 JSON 暴露到 UI，便于现场诊断
            val preview = update.stateJson.take(300).replace("\"", "'")
            CoreMutation(
                state = CoreAppState(
                    ui = CoreUiState(errorMessage = "$err\nJSON 预览: $preview...")
                ),
                effects = emptyList()
            )
        }
    }
}
