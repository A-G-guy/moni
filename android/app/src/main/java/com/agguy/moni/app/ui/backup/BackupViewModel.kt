package com.agguy.moni.app.ui.backup

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.BuildConfig
import com.agguy.moni.core.RustCoreController
import com.agguy.moni.core.platform.AppRestarter
import com.agguy.moni.core.platform.BackupCrypto
import com.agguy.moni.core.platform.BackupHelper
import com.agguy.moni.core.platform.DataStoreHelper
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 备份操作状态。
 */
sealed class BackupOperationState {
    data object Idle : BackupOperationState()
    data class Running(val stage: String, val percent: Int) : BackupOperationState()
    data class Success(val message: String) : BackupOperationState()
    data class Error(val message: String) : BackupOperationState()
}

/**
 * 应用内备份列表项。
 */
data class BackupItem(
    val file: File,
    val recordCount: Long? = null,
    val categoryCount: Long? = null,
)

class BackupViewModel(
    application: Application,
    private val rustCore: RustCoreController,
) : AndroidViewModel(application) {

    private val _operationState = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)
    val operationState: StateFlow<BackupOperationState> = _operationState.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupItem>>(emptyList())
    val backups: StateFlow<List<BackupItem>> = _backups.asStateFlow()

    private val _inspectResult = MutableStateFlow<uniffi.moni_core.BackupInspection?>(null)
    val inspectResult: StateFlow<uniffi.moni_core.BackupInspection?> = _inspectResult.asStateFlow()

    init {
        refreshBackupList()
    }

    fun refreshBackupList() {
        _backups.value = BackupHelper.listBackups(getApplication<Application>()).map {
            BackupItem(file = it)
        }
    }

    /**
     * 导出到应用内备份目录。
     */
    fun exportToInternal() {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            val plainFile = cacheFile("moni_export_plain", ".zip")
            try {
                val outFile = BackupHelper.generateBackupFile(getApplication<Application>())
                val settingsJson = DataStoreHelper.snapshotJson(getApplication<Application>())
                val report = rustCore.backupExport(
                    outZipPath = plainFile.absolutePath,
                    settingsJson = settingsJson,
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                    deviceManufacturer = Build.MANUFACTURER,
                    deviceModel = Build.MODEL,
                    androidSdk = Build.VERSION.SDK_INT,
                    progress = progressListener(),
                )
                BackupCrypto.encryptFile(plainFile, outFile)
                refreshBackupList()
                _operationState.value = BackupOperationState.Success(
                    "Backup complete: ${report.recordCount} records, ${report.categoryCount} categories"
                )
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Backup failed: ${e.message}")
            } finally {
                plainFile.delete()
            }
        }
    }

    /**
     * 导出到 SAF URI（先生成明文缓存，再写入加密备份）。
     */
    fun exportToSaf(uri: Uri) {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            val plainFile = cacheFile("moni_export_plain", ".zip")
            val encryptedFile = cacheFile("moni_export", ".mbak")
            try {
                val settingsJson = DataStoreHelper.snapshotJson(getApplication<Application>())
                rustCore.backupExport(
                    outZipPath = plainFile.absolutePath,
                    settingsJson = settingsJson,
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                    deviceManufacturer = Build.MANUFACTURER,
                    deviceModel = Build.MODEL,
                    androidSdk = Build.VERSION.SDK_INT,
                    progress = progressListener(),
                )
                BackupCrypto.encryptFile(plainFile, encryptedFile)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    encryptedFile.inputStream().use { inp ->
                        inp.copyTo(out)
                    }
                }
                _operationState.value = BackupOperationState.Success("Exported to selected location")
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Export failed: ${e.message}")
            } finally {
                plainFile.delete()
                encryptedFile.delete()
            }
        }
    }

    /**
     * 从 SAF URI 导入（先复制加密文件，再解密到缓存后调用 Rust 恢复）。
     */
    fun importFromSaf(uri: Uri, dbPath: String) {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            val encryptedFile = cacheFile("moni_import", ".mbak")
            val plainFile = cacheFile("moni_import_plain", ".zip")
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inp ->
                    encryptedFile.outputStream().use { out ->
                        inp.copyTo(out)
                    }
                }
                BackupCrypto.decryptFile(encryptedFile, plainFile)
                val report = rustCore.backupRestore(
                    inZipPath = plainFile.absolutePath,
                    dbPath = dbPath,
                    progress = progressListener(),
                )
                DataStoreHelper.restoreFromJson(getApplication<Application>(), report.settingsJson)
                _operationState.value = BackupOperationState.Success(
                    "Restore complete: ${report.restoredRecordCount} records, ${report.restoredCategoryCount} categories"
                )
                encryptedFile.delete()
                plainFile.delete()
                kotlinx.coroutines.delay(800)
                AppRestarter.restartApp(getApplication<Application>())
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Restore failed: ${e.message}")
            } finally {
                encryptedFile.delete()
                plainFile.delete()
            }
        }
    }

    /**
     * 从应用内备份恢复。
     */
    fun restoreFromInternal(file: File, dbPath: String) {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            val plainFile = cacheFile("moni_restore_plain", ".zip")
            try {
                BackupCrypto.decryptFile(file, plainFile)
                val report = rustCore.backupRestore(
                    inZipPath = plainFile.absolutePath,
                    dbPath = dbPath,
                    progress = progressListener(),
                )
                DataStoreHelper.restoreFromJson(getApplication<Application>(), report.settingsJson)
                _operationState.value = BackupOperationState.Success(
                    "Restore complete: ${report.restoredRecordCount} records, ${report.restoredCategoryCount} categories"
                )
                plainFile.delete()
                kotlinx.coroutines.delay(800)
                AppRestarter.restartApp(getApplication<Application>())
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Restore failed: ${e.message}")
            } finally {
                plainFile.delete()
            }
        }
    }

    /**
     * 预览备份包信息。
     */
    fun inspectBackup(file: File) {
        viewModelScope.launch {
            val plainFile = cacheFile("moni_inspect_plain", ".zip")
            try {
                BackupCrypto.decryptFile(file, plainFile)
                _inspectResult.value = rustCore.backupInspect(plainFile.absolutePath)
            } catch (e: Exception) {
                _inspectResult.value = null
            } finally {
                plainFile.delete()
            }
        }
    }

    fun inspectBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val encryptedFile = cacheFile("moni_inspect", ".mbak")
            val plainFile = cacheFile("moni_inspect_plain", ".zip")
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inp ->
                    encryptedFile.outputStream().use { out ->
                        inp.copyTo(out)
                    }
                }
                BackupCrypto.decryptFile(encryptedFile, plainFile)
                _inspectResult.value = rustCore.backupInspect(plainFile.absolutePath)
            } catch (e: Exception) {
                _inspectResult.value = null
                _operationState.value = BackupOperationState.Error("Cannot read backup file: ${e.message}")
            } finally {
                encryptedFile.delete()
                plainFile.delete()
            }
        }
    }

    fun clearInspectResult() {
        _inspectResult.value = null
    }

    fun deleteBackup(file: File) {
        BackupHelper.deleteBackup(file)
        refreshBackupList()
    }

    fun shareBackup(file: File) {
        BackupHelper.shareBackup(getApplication<Application>(), file)
    }

    fun resetState() {
        _operationState.value = BackupOperationState.Idle
    }

    private fun progressListener(): uniffi.moni_core.BackupProgressListener =
        object : uniffi.moni_core.BackupProgressListener {
            override fun onStage(stage: String, percent: Int) {
                _operationState.value = BackupOperationState.Running(stage, percent)
            }
        }

    private fun cacheFile(prefix: String, suffix: String): File =
        File(getApplication<Application>().cacheDir, "${prefix}_${UUID.randomUUID()}$suffix")
}
