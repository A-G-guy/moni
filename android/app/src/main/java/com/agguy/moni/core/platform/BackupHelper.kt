package com.agguy.moni.core.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 备份文件管理帮助类。
 *
 * 负责应用内备份目录的创建、备份文件名生成、列出/删除/分享备份文件。
 */
object BackupHelper {

    private const val BACKUP_DIR = "backups"
    private const val BACKUP_PREFIX = "Moni_Backup_"
    private const val BACKUP_SUFFIX = ".zip"

    /**
     * 获取应用内备份目录，不存在则自动创建。
     */
    fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, BACKUP_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 生成新的备份文件名（含完整路径）。
     */
    fun generateBackupFile(context: Context): File {
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        )
        return File(getBackupDir(context), "${BACKUP_PREFIX}${timestamp}${BACKUP_SUFFIX}")
    }

    /**
     * 列出应用内所有备份文件（按修改时间降序）。
     */
    fun listBackups(context: Context): List<File> {
        val dir = getBackupDir(context)
        return dir.listFiles { file ->
            file.isFile && file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(BACKUP_SUFFIX)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 删除指定备份文件。
     */
    fun deleteBackup(file: File): Boolean {
        return file.delete()
    }

    /**
     * 通过系统分享发送备份文件。
     *
     * 注意：此辅助方法可能被 Application context 调用，因此 intent 必须附加
     * [Intent.FLAG_ACTIVITY_NEW_TASK]，否则在非 Activity context 上启动 Activity
     * 会抛出 [AndroidRuntimeException]。
     */
    fun shareBackup(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享备份").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
