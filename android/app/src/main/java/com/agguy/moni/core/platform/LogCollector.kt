package com.agguy.moni.core.platform

import android.os.Process
import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日志等级。
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * 单条日志条目。
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

/**
 * 日志过滤条件。
 */
data class LogFilter(
    val includeDebug: Boolean = true,
    val includeInfo: Boolean = true,
    val includeWarn: Boolean = true,
    val includeError: Boolean = true
)

/**
 * 内存日志收集器。
 *
 * 维护最近 500 条日志的环形缓冲区，供开发者选项导出使用。
 * 不持久化到文件，仅在内存中保存，避免影响性能。
 */
object LogCollector {
    private const val MAX_SIZE = 500
    private val buffer = ArrayDeque<LogEntry>(MAX_SIZE)
    private val dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    @Synchronized
    fun d(tag: String, message: String) {
        addEntry(LogEntry(System.currentTimeMillis(), LogLevel.DEBUG, tag, message))
    }

    @Synchronized
    fun i(tag: String, message: String) {
        addEntry(LogEntry(System.currentTimeMillis(), LogLevel.INFO, tag, message))
    }

    @Synchronized
    fun w(tag: String, message: String) {
        addEntry(LogEntry(System.currentTimeMillis(), LogLevel.WARN, tag, message))
    }

    @Synchronized
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        addEntry(LogEntry(System.currentTimeMillis(), LogLevel.ERROR, tag, message, throwable))
    }

    @Synchronized
    fun getLogs(filter: LogFilter = LogFilter()): List<LogEntry> {
        return buffer.filter { entry ->
            when (entry.level) {
                LogLevel.DEBUG -> filter.includeDebug
                LogLevel.INFO -> filter.includeInfo
                LogLevel.WARN -> filter.includeWarn
                LogLevel.ERROR -> filter.includeError
            }
        }.reversed() // 倒序：最新的在最前面
    }

    @Synchronized
    fun clear() {
        buffer.clear()
    }

    @Synchronized
    fun formatLogs(filter: LogFilter = LogFilter()): String {
        val logs = getLogs(filter)
        val sb = StringBuilder()
        logs.forEach { entry ->
            val time = dateFormatter.format(Instant.ofEpochMilli(entry.timestamp))
            val level = entry.level.name.first()
            sb.append("[$time] $level/${entry.tag}: ${entry.message}")
            entry.throwable?.let {
                sb.append("\n").append(Log.getStackTraceString(it))
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * 收集当前进程的系统日志（含 Rust 层通过 android_logger 写入的日志）。
     * 由于 Android 13+ 限制，可能返回空或失败提示。
     */
    fun collectProcessLogcat(): String = try {
        val pid = Process.myPid().toString()
        val process = Runtime.getRuntime().exec(
            arrayOf("logcat", "-d", "--pid=$pid", "-v", "threadtime")
        )
        process.inputStream.bufferedReader().use { reader ->
            val lines = reader.readLines()
            if (lines.isEmpty()) {
                "[系统日志为空，可能受 Android 权限限制]"
            } else {
                lines.joinToString("\n")
            }
        }
    } catch (e: Exception) {
        "[无法读取系统日志: ${e.message}]"
    }

    private fun addEntry(entry: LogEntry) {
        if (buffer.size >= MAX_SIZE) {
            buffer.removeFirst()
        }
        buffer.addLast(entry)
    }
}
