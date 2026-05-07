package com.agguy.moni.core.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 自动备份 WorkManager 调度器。
 *
 * 根据 [frequency] 注册或取消周期性备份任务：
 * - "every_launch"：取消周期任务（由应用启动时手动触发）
 * - "daily"：注册 1 天周期任务
 * - "weekly"：注册 7 天周期任务
 * - "monthly"：注册 30 天周期任务（WorkManager 最小周期为 15 分钟）
 */
object AutoBackupScheduler {

    private const val WORK_TAG = "moni_auto_backup"

    /**
     * 根据当前配置重新注册自动备份任务。
     *
     * 若配置为 "every_launch" 或未启用，则取消已有的周期任务。
     */
    fun schedule(context: Context, enabled: Boolean, frequency: String) {
        try {
            val workManager = WorkManager.getInstance(context)

            if (!enabled || frequency == "every_launch") {
                workManager.cancelAllWorkByTag(WORK_TAG)
                LogCollector.i("AutoBackupScheduler", "已取消周期备份任务")
                return
            }

            val repeatInterval = when (frequency) {
                "daily" -> 1L
                "weekly" -> 7L
                "monthly" -> 30L
                else -> 1L
            }

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                repeatInterval,
                TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

            LogCollector.i("AutoBackupScheduler", "已注册周期备份任务: $frequency, 间隔=${repeatInterval}天")
        } catch (e: Exception) {
            LogCollector.e("AutoBackupScheduler", "注册周期备份任务失败", e)
        }
    }

    /**
     * 立即触发一次性的自动备份（用于"每次启动"场景）。
     *
     * 使用 [ExistingWorkPolicy.KEEP] 避免应用频繁重启时任务堆积。
     */
    fun triggerOnce(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val request = androidx.work.OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .addTag(WORK_TAG)
                .build()
            workManager.enqueueUniqueWork(
                "${WORK_TAG}_once",
                ExistingWorkPolicy.KEEP,
                request
            )
            LogCollector.i("AutoBackupScheduler", "已触发一次性自动备份")
        } catch (e: Exception) {
            LogCollector.e("AutoBackupScheduler", "触发一次性备份失败", e)
        }
    }
}
