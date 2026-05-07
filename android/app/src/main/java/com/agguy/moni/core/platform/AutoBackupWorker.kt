package com.agguy.moni.core.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agguy.moni.BuildConfig
import com.agguy.moni.core.RustCoreController
import kotlinx.coroutines.flow.first

/**
 * 自动备份 Worker。
 *
 * 从 DataStore 读取配置，调用 Rust 核心完成备份决策、执行与清理。
 * 若配置了外部目录复制，通过 SAF 将备份复制到指定位置。
 *
 * 注意：每次执行都会创建独立的 [RustCoreController] 并初始化到实际数据库路径，
 * 以确保 Worker 在独立进程中也能正确访问数据。
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext

        // 1. 读取配置
        val enabled = DataStoreHelper.autoBackupEnabledFlow(ctx).first()
        if (!enabled) {
            LogCollector.i("AutoBackupWorker", "自动备份未启用，跳过")
            return Result.success()
        }

        val frequency = DataStoreHelper.autoBackupFrequencyFlow(ctx).first()
        val maxCount = DataStoreHelper.autoBackupMaxCountFlow(ctx).first()
        val copyToExternal = DataStoreHelper.autoBackupCopyToExternalFlow(ctx).first()
        val externalUri = DataStoreHelper.autoBackupExternalUriFlow(ctx).first()
        val lastTime = DataStoreHelper.autoBackupLastTimeFlow(ctx).first()

        // 2. 初始化 Rust 核心到实际数据库
        val dbPath = ctx.filesDir.absolutePath + "/moni.db"
        val rustCore = RustCoreController()
        try {
            rustCore.initializeWithDb(dbPath)
        } catch (e: Exception) {
            LogCollector.e("AutoBackupWorker", "初始化数据库失败", e)
            return Result.retry()
        }

        // 3. 决策是否执行备份
        val shouldRun = try {
            rustCore.autoBackupShouldRun(lastTime, frequency)
        } catch (e: Exception) {
            LogCollector.e("AutoBackupWorker", "备份决策失败", e)
            return Result.retry()
        }

        if (!shouldRun) {
            LogCollector.i("AutoBackupWorker", "不满足备份条件，跳过")
            return Result.success()
        }

        // 4. 执行备份
        val backupDir = BackupHelper.getBackupDir(ctx).absolutePath
        val settingsJson = try {
            DataStoreHelper.snapshotJson(ctx)
        } catch (e: Exception) {
            LogCollector.e("AutoBackupWorker", "读取设置失败", e)
            "{}"
        }

        val report = try {
            rustCore.autoBackupPerform(
                backupDir = backupDir,
                settingsJson = settingsJson,
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                deviceManufacturer = android.os.Build.MANUFACTURER,
                deviceModel = android.os.Build.MODEL,
                androidSdk = android.os.Build.VERSION.SDK_INT,
            )
        } catch (e: Exception) {
            LogCollector.e("AutoBackupWorker", "自动备份执行失败", e)
            return Result.retry()
        }

        LogCollector.i(
            "AutoBackupWorker",
            "自动备份完成: ${report.zipPath}, " +
                "records=${report.recordCount}, categories=${report.categoryCount}"
        )

        // 5. 复制到外部目录（若配置）
        if (copyToExternal && externalUri != null) {
            try {
                val uri = android.net.Uri.parse(externalUri)
                val backupFile = java.io.File(report.zipPath)
                copyToSafUri(ctx, backupFile, uri)
                LogCollector.i("AutoBackupWorker", "已复制到外部目录")
            } catch (e: Exception) {
                LogCollector.e("AutoBackupWorker", "复制到外部目录失败", e)
                // 复制失败不影响本地备份成功状态
            }
        }

        // 6. 清理旧备份
        try {
            val removed = rustCore.autoBackupCleanup(backupDir, maxCount.toUInt())
            if (removed > 0u) {
                LogCollector.i("AutoBackupWorker", "清理旧备份: $removed 个")
            }
        } catch (e: Exception) {
            LogCollector.e("AutoBackupWorker", "清理旧备份失败", e)
        }

        // 7. 更新上次备份时间
        try {
            DataStoreHelper.saveAutoBackupLastTime(ctx, report.createdAt)
        } catch (e: Exception) {
            LogCollector.e("AutoBackupWorker", "更新上次备份时间失败", e)
        }

        return Result.success()
    }

    /**
     * 将文件复制到 SAF URI 指定的目录中。
     */
    private fun copyToSafUri(context: Context, sourceFile: java.io.File, dirUri: android.net.Uri) {
        val docUri = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, dirUri)
            ?: throw IllegalStateException("无法解析 SAF URI")
        val mimeType = "application/zip"
        val destFile = docUri.createFile(mimeType, sourceFile.name)
            ?: throw IllegalStateException("无法在外部目录创建文件")
        context.contentResolver.openOutputStream(destFile.uri)?.use { out ->
            sourceFile.inputStream().use { inp ->
                inp.copyTo(out)
            }
        } ?: throw IllegalStateException("无法打开输出流")
    }
}
