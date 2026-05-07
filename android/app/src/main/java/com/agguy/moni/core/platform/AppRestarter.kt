package com.agguy.moni.core.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process

/**
 * 应用重启工具。
 *
 * 清空数据后调用，强制应用回到初始状态。
 */
object AppRestarter {

    /**
     * 重启当前应用。
     *
     * 通过 AlarmManager 延迟 500ms 启动 MainActivity 并清除任务栈，
     * 确保新 Activity 有足够时间在当前进程被杀前完成启动注册。
     */
    fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent)
        }
        Process.killProcess(Process.myPid())
    }
}
