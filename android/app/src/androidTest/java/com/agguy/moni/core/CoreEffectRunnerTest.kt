package com.agguy.moni.core

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** CoreEffectRunner 仪器测试 — 需 Application Context。 */
class CoreEffectRunnerTest {

    private fun createRunner() = CoreEffectRunner(ApplicationProvider.getApplicationContext())

    @Test
    fun log_effect_does_not_invoke_callbacks() {
        val runner = createRunner()
        var snackbarMessage: String? = null
        var navigateScreen: String? = null
        runner.onShowSnackbar = { snackbarMessage = it }
        runner.onNavigate = { navigateScreen = it }

        runner.runEffect(CoreEffect(kind = "log", payloadJson = "{\"any\":\"value\"}"))

        assertNull("log 不应触发 snackbar 回调", snackbarMessage)
        assertNull("log 不应触发 navigate 回调", navigateScreen)
    }

    @Test
    fun show_snackbar_parses_message_and_invokes_callback() {
        val runner = createRunner()
        var captured: String? = null
        runner.onShowSnackbar = { captured = it }

        runner.runEffect(
            CoreEffect(kind = "show_snackbar", payloadJson = "{\"message\":\"测试消息\"}")
        )

        assertEquals("测试消息", captured)
    }

    @Test
    fun show_snackbar_with_malformed_payload_falls_back_to_raw_payload() {
        val runner = createRunner()
        var captured: String? = null
        runner.onShowSnackbar = { captured = it }

        val raw = "this is not json"
        runner.runEffect(CoreEffect(kind = "show_snackbar", payloadJson = raw))
        assertEquals(raw, captured)
    }

    @Test
    fun show_snackbar_without_message_field_falls_back_to_raw_payload() {
        val runner = createRunner()
        var captured: String? = null
        runner.onShowSnackbar = { captured = it }

        val raw = "{\"foo\":\"bar\"}"
        runner.runEffect(CoreEffect(kind = "show_snackbar", payloadJson = raw))
        assertEquals(raw, captured)
    }

    @Test
    fun navigate_parses_screen_and_invokes_callback() {
        val runner = createRunner()
        var captured: String? = null
        runner.onNavigate = { captured = it }

        runner.runEffect(
            CoreEffect(kind = "navigate", payloadJson = "{\"screen\":\"settings\"}")
        )

        assertEquals("settings", captured)
    }

    @Test
    fun navigate_with_malformed_payload_falls_back_to_empty_string() {
        val runner = createRunner()
        var captured: String? = null
        runner.onNavigate = { captured = it }

        runner.runEffect(CoreEffect(kind = "navigate", payloadJson = "not json"))

        assertEquals("", captured)
    }

    @Test
    fun unknown_effect_kind_is_ignored_without_invoking_callbacks() {
        val runner = createRunner()
        var snackbarMessage: String? = null
        var navigateScreen: String? = null
        runner.onShowSnackbar = { snackbarMessage = it }
        runner.onNavigate = { navigateScreen = it }

        runner.runEffect(CoreEffect(kind = "totally_unknown_effect", payloadJson = "{}"))

        assertNull(snackbarMessage)
        assertNull(navigateScreen)
    }

    @Test
    fun callbacks_can_be_left_null_and_runEffect_should_not_crash() {
        val runner = createRunner()
        runner.runEffect(
            CoreEffect(kind = "show_snackbar", payloadJson = "{\"message\":\"x\"}")
        )
        runner.runEffect(
            CoreEffect(kind = "navigate", payloadJson = "{\"screen\":\"x\"}")
        )
    }

    @Test
    fun multiple_effects_are_processed_in_sequence() {
        val runner = createRunner()
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
    fun callback_override_replaces_previous_reference() {
        val runner = createRunner()
        val first: (String) -> Unit = {}
        val second: (String) -> Unit = {}
        runner.onShowSnackbar = first
        runner.onShowSnackbar = second
        assertSame(second, runner.onShowSnackbar)
    }
}