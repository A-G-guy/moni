package com.agguy.moni.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * `CoreEffectRunner` 单元测试，覆盖 `runEffect` 的全部分支：
 * - `log`：仅打日志（无回调），不应调用任何 onShowSnackbar/onNavigate
 * - `show_snackbar`：解析 payload 中的 `message` 并触发回调
 * - `show_snackbar` payload 异常：回退为原始 payload 字符串作为消息
 * - `navigate`：解析 payload 中的 `screen` 并触发回调
 * - `navigate` payload 异常：传入空字符串
 * - 未知 kind：不应调用任何回调
 */
class CoreEffectRunnerTest {

    @Test
    fun `log effect does not invoke callbacks`() {
        val runner = CoreEffectRunner()
        var snackbarMessage: String? = null
        var navigateScreen: String? = null
        runner.onShowSnackbar = { snackbarMessage = it }
        runner.onNavigate = { navigateScreen = it }

        runner.runEffect(CoreEffect(kind = "log", payloadJson = "{\"any\":\"value\"}"))

        assertNull("log 不应触发 snackbar 回调", snackbarMessage)
        assertNull("log 不应触发 navigate 回调", navigateScreen)
    }

    @Test
    fun `show_snackbar parses message and invokes callback`() {
        val runner = CoreEffectRunner()
        var captured: String? = null
        runner.onShowSnackbar = { captured = it }

        runner.runEffect(
            CoreEffect(kind = "show_snackbar", payloadJson = "{\"message\":\"测试消息\"}")
        )

        assertEquals("测试消息", captured)
    }

    @Test
    fun `show_snackbar with malformed payload falls back to raw payload`() {
        val runner = CoreEffectRunner()
        var captured: String? = null
        runner.onShowSnackbar = { captured = it }

        // 非 JSON：解析失败，按设计回退为原始 payload
        val raw = "this is not json"
        runner.runEffect(CoreEffect(kind = "show_snackbar", payloadJson = raw))
        assertEquals(raw, captured)
    }

    @Test
    fun `show_snackbar without message field falls back to raw payload`() {
        val runner = CoreEffectRunner()
        var captured: String? = null
        runner.onShowSnackbar = { captured = it }

        val raw = "{\"foo\":\"bar\"}"
        runner.runEffect(CoreEffect(kind = "show_snackbar", payloadJson = raw))
        assertEquals(raw, captured)
    }

    @Test
    fun `navigate parses screen and invokes callback`() {
        val runner = CoreEffectRunner()
        var captured: String? = null
        runner.onNavigate = { captured = it }

        runner.runEffect(
            CoreEffect(kind = "navigate", payloadJson = "{\"screen\":\"settings\"}")
        )

        assertEquals("settings", captured)
    }

    @Test
    fun `navigate with malformed payload falls back to empty string`() {
        val runner = CoreEffectRunner()
        var captured: String? = null
        runner.onNavigate = { captured = it }

        runner.runEffect(CoreEffect(kind = "navigate", payloadJson = "not json"))

        assertEquals("", captured)
    }

    @Test
    fun `unknown effect kind is ignored without invoking callbacks`() {
        val runner = CoreEffectRunner()
        var snackbarMessage: String? = null
        var navigateScreen: String? = null
        runner.onShowSnackbar = { snackbarMessage = it }
        runner.onNavigate = { navigateScreen = it }

        runner.runEffect(CoreEffect(kind = "totally_unknown_effect", payloadJson = "{}"))

        assertNull(snackbarMessage)
        assertNull(navigateScreen)
    }

    @Test
    fun `callbacks can be left null and runEffect should not crash`() {
        // 验证 lambda 为 null 时使用 ?.invoke 安全调用
        val runner = CoreEffectRunner()
        // 不要 setter 任何回调
        runner.runEffect(
            CoreEffect(kind = "show_snackbar", payloadJson = "{\"message\":\"x\"}")
        )
        runner.runEffect(
            CoreEffect(kind = "navigate", payloadJson = "{\"screen\":\"x\"}")
        )
        // 通过：未抛异常即视为 OK
    }

    @Test
    fun `multiple effects are processed in sequence`() {
        val runner = CoreEffectRunner()
        val collected = mutableListOf<String>()
        runner.onShowSnackbar = { collected += "snack:$it" }
        runner.onNavigate = { collected += "nav:$it" }

        listOf(
            CoreEffect("show_snackbar", "{\"message\":\"a\"}"),
            CoreEffect("navigate", "{\"screen\":\"home\"}"),
            CoreEffect("show_snackbar", "{\"message\":\"b\"}"),
        ).forEach(runner::runEffect)

        assertEquals(listOf("snack:a", "nav:home", "snack:b"), collected)
    }

    @Test
    fun `callback override replaces previous reference`() {
        val runner = CoreEffectRunner()
        val first: (String) -> Unit = {}
        val second: (String) -> Unit = {}
        runner.onShowSnackbar = first
        runner.onShowSnackbar = second
        assertSame(second, runner.onShowSnackbar)
    }
}
