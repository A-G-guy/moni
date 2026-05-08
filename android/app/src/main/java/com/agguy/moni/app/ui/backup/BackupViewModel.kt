package com.agguy.moni.app.ui.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.BuildConfig
import com.agguy.moni.core.RustCoreController
import com.agguy.moni.core.platform.AppRestarter
import com.agguy.moni.core.platform.BackupHelper
import com.agguy.moni.core.platform.DataStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

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
            try {
                val outFile = BackupHelper.generateBackupFile(getApplication<Application>())
                val settingsJson = DataStoreHelper.snapshotJson(getApplication<Application>())
                val report = rustCore.backupExport(
                    outZipPath = outFile.absolutePath,
                    settingsJson = settingsJson,
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                    deviceManufacturer = Build.MANUFACTURER,
                    deviceModel = Build.MODEL,
                    androidSdk = Build.VERSION.SDK_INT,
                    progress = object : uniffi.moni_core.BackupProgressListener {
                        override fun onStage(stage: String, percent: Int) {
                            _operationState.value = BackupOperationState.Running(stage, percent)
                        }
                    },
                )
                refreshBackupList()
                _operationState.value = BackupOperationState.Success(
                    "Backup complete: ${report.recordCount} records, ${report.categoryCount} categories"
                )
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Backup failed: ${e.message}")
            }
        }
    }

    /**
     * 导出到 SAF URI（先将备份生成到缓存，再复制到 SAF）。
     */
    fun exportToSaf(uri: Uri) {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            val cacheFile = File(getApplication<Application>().cacheDir, "moni_export_${UUID.randomUUID()}.zip")
            try {
                val settingsJson = DataStoreHelper.snapshotJson(getApplication<Application>())
                rustCore.backupExport(
                    outZipPath = cacheFile.absolutePath,
                    settingsJson = settingsJson,
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                    deviceManufacturer = Build.MANUFACTURER,
                    deviceModel = Build.MODEL,
                    androidSdk = Build.VERSION.SDK_INT,
                    progress = object : uniffi.moni_core.BackupProgressListener {
                        override fun onStage(stage: String, percent: Int) {
                            _operationState.value = BackupOperationState.Running(stage, percent)
                        }
                    },
                )
                // 复制到 SAF URI
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    cacheFile.inputStream().use { inp ->
                        inp.copyTo(out)
                    }
                }
                _operationState.value = BackupOperationState.Success("Exported to selected location")
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Export failed: ${e.message}")
            } finally {
                if (cacheFile.exists()) cacheFile.delete()
            }
        }
    }

    /**
     * 从 SAF URI 导入（先将内容复制到缓存，再调用 Rust 恢复）。
     */
    fun importFromSaf(uri: Uri, dbPath: String) {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            val cacheFile = File(getApplication<Application>().cacheDir, "moni_import_${UUID.randomUUID()}.zip")
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inp ->
                    cacheFile.outputStream().use { out ->
                        inp.copyTo(out)
                    }
                }
                val report = rustCore.backupRestore(
                    inZipPath = cacheFile.absolutePath,
                    dbPath = dbPath,
                    progress = object : uniffi.moni_core.BackupProgressListener {
                        override fun onStage(stage: String, percent: Int) {
                            _operationState.value = BackupOperationState.Running(stage, percent)
                        }
                    },
                )
                // 恢复 DataStore 设置
                DataStoreHelper.restoreFromJson(getApplication<Application>(), report.settingsJson)
                _operationState.value = BackupOperationState.Success(
                    "Restore complete: ${report.restoredRecordCount} records, ${report.restoredCategoryCount} categories"
                )
                // 延迟重启，让 Snackbar 有机会显示
                kotlinx.coroutines.delay(800)
                AppRestarter.restartApp(getApplication<Application>())
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Restore failed: ${e.message}")
            } finally {
                if (cacheFile.exists()) cacheFile.delete()
            }
        }
    }

    /**
     * 从应用内备份恢复。
     */
    fun restoreFromInternal(file: File, dbPath: String) {
        viewModelScope.launch {
            _operationState.value = BackupOperationState.Running("Preparing...", 0)
            try {
                val report = rustCore.backupRestore(
                    inZipPath = file.absolutePath,
                    dbPath = dbPath,
                    progress = object : uniffi.moni_core.BackupProgressListener {
                        override fun onStage(stage: String, percent: Int) {
                            _operationState.value = BackupOperationState.Running(stage, percent)
                        }
                    },
                )
                DataStoreHelper.restoreFromJson(getApplication<Application>(), report.settingsJson)
                _operationState.value = BackupOperationState.Success(
                    "Restore complete: ${report.restoredRecordCount} records, ${report.restoredCategoryCount} categories"
                )
                kotlinx.coroutines.delay(800)
                AppRestarter.restartApp(getApplication<Application>())
            } catch (e: Exception) {
                _operationState.value = BackupOperationState.Error("Restore failed: ${e.message}")
            }
        }
    }

    /**
     * 预览备份包信息。
     */
    fun inspectBackup(file: File) {
        viewModelScope.launch {
            try {
                _inspectResult.value = rustCore.backupInspect(file.absolutePath)
            } catch (e: Exception) {
                _inspectResult.value = null
            }
        }
    }

    fun inspectBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val cacheFile = File(getApplication<Application>().cacheDir, "moni_inspect_${UUID.randomUUID()}.zip")
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inp ->
                    cacheFile.outputStream().use { out ->
                        inp.copyTo(out)
                    }
                }
                _inspectResult.value = rustCore.backupInspect(cacheFile.absolutePath)
            } catch (e: Exception) {
                _inspectResult.value = null
                _operationState.value = BackupOperationState.Error("Cannot read backup file: ${e.message}")
            } finally {
                cacheFile.delete()
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
}
