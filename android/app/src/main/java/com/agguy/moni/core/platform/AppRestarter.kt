package com.agguy.moni.core.platform

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
     * 通过重新启动 MainActivity 并清除任务栈实现软重启，
     * 然后结束当前进程。
     */
    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
        Process.killProcess(Process.myPid())
    }
}
