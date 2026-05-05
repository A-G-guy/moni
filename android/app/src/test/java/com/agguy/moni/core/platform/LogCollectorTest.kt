package com.agguy.moni.core.platform

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `LogCollector` 单元测试，覆盖：
 * - d/i/w/e 各级别写入；
 * - getLogs 默认与按级别过滤；
 * - 倒序返回（最新在前）；
 * - clear；
 * - 环形缓冲达到 MAX_SIZE (500) 时的丢弃最旧逻辑；
 * - formatLogs 包含时间戳/级别/tag/消息组合。
 *
 * 注：`LogCollector` 是 object（单例），多个测试间需 `clear()` 重置。
 */
class LogCollectorTest {

    @After
    fun tearDown() {
        // 确保各测试间互不干扰
        LogCollector.clear()
    }

    @Test
    fun `d i w e all append entries with correct level`() {
        LogCollector.clear()
        LogCollector.d("tag1", "msg-debug")
        LogCollector.i("tag2", "msg-info")
        LogCollector.w("tag3", "msg-warn")
        LogCollector.e("tag4", "msg-error")

        val logs = LogCollector.getLogs()
        // 倒序：最新在前
        assertEquals(4, logs.size)
        assertEquals(LogLevel.ERROR, logs[0].level)
        assertEquals("tag4", logs[0].tag)
        assertEquals("msg-error", logs[0].message)
        assertEquals(LogLevel.WARN, logs[1].level)
        assertEquals(LogLevel.INFO, logs[2].level)
        assertEquals(LogLevel.DEBUG, logs[3].level)
    }

    @Test
    fun `e captures throwable`() {
        LogCollector.clear()
        val ex = IllegalStateException("boom")
        LogCollector.e("X", "出错了", ex)
        val logs = LogCollector.getLogs()
        assertEquals(1, logs.size)
        assertEquals("出错了", logs[0].message)
        assertEquals(ex, logs[0].throwable)
    }

    @Test
    fun `clear empties the buffer`() {
        LogCollector.clear()
        LogCollector.d("tag", "msg")
        assertFalse(LogCollector.getLogs().isEmpty())

        LogCollector.clear()
        assertTrue(LogCollector.getLogs().isEmpty())
    }

    @Test
    fun `filter excludes specified levels`() {
        LogCollector.clear()
        LogCollector.d("tag", "d-msg")
        LogCollector.i("tag", "i-msg")
        LogCollector.w("tag", "w-msg")
        LogCollector.e("tag", "e-msg")

        val onlyError = LogCollector.getLogs(
            LogFilter(
                includeDebug = false,
                includeInfo = false,
                includeWarn = false,
                includeError = true,
            )
        )
        assertEquals(1, onlyError.size)
        assertEquals(LogLevel.ERROR, onlyError[0].level)

        val warnAndError = LogCollector.getLogs(
            LogFilter(includeDebug = false, includeInfo = false)
        )
        assertEquals(2, warnAndError.size)
        // 倒序
        assertEquals(LogLevel.ERROR, warnAndError[0].level)
        assertEquals(LogLevel.WARN, warnAndError[1].level)
    }

    @Test
    fun `filter that excludes everything returns empty`() {
        LogCollector.clear()
        LogCollector.d("tag", "x")
        LogCollector.i("tag", "x")
        val none = LogCollector.getLogs(
            LogFilter(false, false, false, false)
        )
        assertTrue(none.isEmpty())
    }

    @Test
    fun `ring buffer drops oldest beyond MAX_SIZE`() {
        LogCollector.clear()
        // MAX_SIZE 私有为 500；写 510 条，前 10 条应被淘汰
        for (i in 0 until 510) {
            LogCollector.d("ring", "msg-$i")
        }
        val logs = LogCollector.getLogs()
        assertEquals(500, logs.size)
        // 倒序：最新条目在前
        assertEquals("msg-509", logs.first().message)
        // 最旧的留下来的应是 msg-10 （索引 9 即第 10 条）
        assertEquals("msg-10", logs.last().message)
    }

    @Test
    fun `formatLogs contains tag and message segments`() {
        LogCollector.clear()
        LogCollector.i("MyTag", "Hello world")
        val text = LogCollector.formatLogs()
        assertTrue("formatLogs 应包含 tag", text.contains("MyTag"))
        assertTrue("formatLogs 应包含 message", text.contains("Hello world"))
        assertTrue("应包含级别首字母 I", text.contains("I/MyTag"))
    }

    @Test
    fun `formatLogs respects filter`() {
        LogCollector.clear()
        LogCollector.d("DTag", "debug-text")
        LogCollector.e("ETag", "error-text")

        val onlyError = LogCollector.formatLogs(
            LogFilter(includeDebug = false, includeInfo = false, includeWarn = false)
        )
        assertFalse("过滤后不应包含 debug 内容", onlyError.contains("debug-text"))
        assertTrue("过滤后应包含 error 内容", onlyError.contains("error-text"))
    }
}
