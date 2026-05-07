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

    private fun decodeMutation(update: uniffi.moni_core.CoreUpdate): CoreMutation {
        return try {
            CoreMutation(
                state = BridgeJson.decodeFromString(CoreAppState.serializer(), update.stateJson),
                effects = update.effects.map { CoreEffect(it.kind, it.payloadJson) }
            )
        } catch (e: Exception) {
            LogCollector.e("RustCoreController", "JSON 反序列化失败: ${e.message}, stateJson=${update.stateJson.take(200)}")
            CoreMutation(
                state = CoreAppState(),
                effects = emptyList()
            )
        }
    }
}
