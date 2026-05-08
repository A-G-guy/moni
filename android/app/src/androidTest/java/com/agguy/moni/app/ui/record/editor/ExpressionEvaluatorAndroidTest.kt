package com.agguy.moni.app.ui.record.editor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agguy.moni.core.RustCoreController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ExpressionEvaluator 端到端集成测试。
 *
 * 验证 Kotlin 薄封装与 Rust 内核之间的 FFI 桥接正确性。
 * 必须在 Android 运行时执行（native 库依赖）。
 */
@RunWith(AndroidJUnit4::class)
class ExpressionEvaluatorAndroidTest {

    @Before
    fun setup() {
        val rustCore = RustCoreController()
        runBlocking {
            rustCore.initialize()
        }
        ExpressionEvaluator.initialize(rustCore)
    }

    @Test
    fun evaluateToCents_纯整数() {
        assertEquals(1500L, ExpressionEvaluator.evaluateToCents("15"))
    }

    @Test
    fun evaluateToCents_一位小数() {
        assertEquals(1550L, ExpressionEvaluator.evaluateToCents("15.5"))
    }

    @Test
    fun evaluateToCents_两位小数() {
        assertEquals(1555L, ExpressionEvaluator.evaluateToCents("15.55"))
    }

    @Test
    fun evaluateToCents_单个加法() {
        assertEquals(3550L, ExpressionEvaluator.evaluateToCents("15.5+20"))
    }

    @Test
    fun evaluateToCents_单个减法() {
        assertEquals(450L, ExpressionEvaluator.evaluateToCents("20-15.5"))
    }

    @Test
    fun evaluateToCents_连续加减() {
        assertEquals(8450L, ExpressionEvaluator.evaluateToCents("100-25.5+10"))
    }

    @Test
    fun evaluateToCents_零() {
        assertEquals(0L, ExpressionEvaluator.evaluateToCents("0"))
    }

    @Test
    fun evaluateToCents_空字符串返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents(""))
    }

    @Test
    fun evaluateToCents_仅有运算符返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents("+"))
    }

    @Test
    fun evaluateToCents_运算符结尾返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents("15+"))
    }

    @Test
    fun evaluateToCents_多个小数点返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents("15..5"))
    }

    @Test
    fun evaluateToCents_超过两位小数返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents("15.555"))
    }

    @Test
    fun evaluateToCents_负数返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents("-15"))
    }

    @Test
    fun evaluateToCents_非法字符返回null() {
        assertNull(ExpressionEvaluator.evaluateToCents("15abc"))
    }

    @Test
    fun hasPendingOperation_无运算符() {
        assertFalse(ExpressionEvaluator.hasPendingOperation("15"))
    }

    @Test
    fun hasPendingOperation_含加号() {
        assertTrue(ExpressionEvaluator.hasPendingOperation("15+20"))
    }

    @Test
    fun hasPendingOperation_以运算符结尾() {
        assertTrue(ExpressionEvaluator.hasPendingOperation("15+"))
    }

    @Test
    fun hasPendingOperation_空字符串() {
        assertFalse(ExpressionEvaluator.hasPendingOperation(""))
    }

    @Test
    fun formatForDisplay_基本格式化() {
        assertEquals("15 + 20", ExpressionEvaluator.formatForDisplay("15+20"))
    }

    @Test
    fun formatForDisplay_连续运算() {
        assertEquals("100 - 25.5 + 10", ExpressionEvaluator.formatForDisplay("100-25.5+10"))
    }

    @Test
    fun formatForDisplay_单个数字不变() {
        assertEquals("15", ExpressionEvaluator.formatForDisplay("15"))
    }
}
