package com.agguy.moni.core

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

    private fun decodeMutation(update: uniffi.moni_core.CoreUpdate): CoreMutation = CoreMutation(
        state = BridgeJson.decodeFromString(CoreAppState.serializer(), update.stateJson),
        effects = update.effects.map { CoreEffect(it.kind, it.payloadJson) }
    )
}
