package com.agguy.moni.app.ui.record.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEvaluatorTest {

    @Test
    fun `evaluateToCents - 纯整数`() {
        assertEquals(1500L, ExpressionEvaluator.evaluateToCents("15"))
    }

    @Test
    fun `evaluateToCents - 一位小数`() {
        assertEquals(1550L, ExpressionEvaluator.evaluateToCents("15.5"))
    }

    @Test
    fun `evaluateToCents - 两位小数`() {
        assertEquals(1555L, ExpressionEvaluator.evaluateToCents("15.55"))
    }

    @Test
    fun `evaluateToCents - 单个加法`() {
        assertEquals(3550L, ExpressionEvaluator.evaluateToCents("15.5+20"))
    }

    @Test
    fun `evaluateToCents - 单个减法`() {
        assertEquals(450L, ExpressionEvaluator.evaluateToCents("20-15.5"))
    }

    @Test
    fun `evaluateToCents - 连续加减`() {
        assertEquals(8450L, ExpressionEvaluator.evaluateToCents("100-25.5+10"))
    }

    @Test
    fun `evaluateToCents - 单个乘法`() {
        assertEquals(30000L, ExpressionEvaluator.evaluateToCents("100×3"))
    }

    @Test
    fun `evaluateToCents - 单个除法`() {
        assertEquals(3333L, ExpressionEvaluator.evaluateToCents("100÷3"))
    }

    @Test
    fun `evaluateToCents - 乘除混合优先级`() {
        assertEquals(5000L, ExpressionEvaluator.evaluateToCents("100-25×2"))
    }

    @Test
    fun `evaluateToCents - 加减乘除混合`() {
        // 100 + (50×3) - (25÷2) = 100 + 150 - 12.5 = 237.5 → 23750 分
        assertEquals(23750L, ExpressionEvaluator.evaluateToCents("100+50×3-25÷2"))
    }

    @Test
    fun `evaluateToCents - 连续乘除`() {
        assertEquals(6667L, ExpressionEvaluator.evaluateToCents("100÷3×2"))
    }

    @Test
    fun `evaluateToCents - 小数乘法`() {
        assertEquals(7650L, ExpressionEvaluator.evaluateToCents("25.5×3"))
    }

    @Test
    fun `evaluateToCents - 零`() {
        assertEquals(0L, ExpressionEvaluator.evaluateToCents("0"))
    }

    @Test
    fun `evaluateToCents - 空字符串返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents(""))
    }

    @Test
    fun `evaluateToCents - 仅有运算符返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("+"))
    }

    @Test
    fun `evaluateToCents - 运算符结尾返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("15+"))
        assertNull(ExpressionEvaluator.evaluateToCents("20×"))
        assertNull(ExpressionEvaluator.evaluateToCents("10÷"))
    }

    @Test
    fun `evaluateToCents - 多个小数点返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("15..5"))
    }

    @Test
    fun `evaluateToCents - 超过两位小数返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("15.555"))
    }

    @Test
    fun `evaluateToCents - 负数返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("-15"))
    }

    @Test
    fun `evaluateToCents - 仅运算符开头返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("-15+10"))
    }

    @Test
    fun `evaluateToCents - 减到负数返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("10-15"))
    }

    @Test
    fun `evaluateToCents - 非法字符返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("15abc"))
    }

    @Test
    fun `evaluateToCents - 连续运算符返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("15++20"))
        assertNull(ExpressionEvaluator.evaluateToCents("10××5"))
    }

    @Test
    fun `evaluateToCents - 除零返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("100÷0"))
    }

    @Test
    fun `evaluateToCents - 小数点后无数字返回 null`() {
        assertNull(ExpressionEvaluator.evaluateToCents("15."))
    }

    @Test
    fun `hasPendingOperation - 无运算符`() {
        assertFalse(ExpressionEvaluator.hasPendingOperation("15"))
    }

    @Test
    fun `hasPendingOperation - 含加号`() {
        assertTrue(ExpressionEvaluator.hasPendingOperation("15+20"))
    }

    @Test
    fun `hasPendingOperation - 含乘号`() {
        assertTrue(ExpressionEvaluator.hasPendingOperation("15×20"))
    }

    @Test
    fun `hasPendingOperation - 含除号`() {
        assertTrue(ExpressionEvaluator.hasPendingOperation("15÷20"))
    }

    @Test
    fun `hasPendingOperation - 以运算符结尾`() {
        assertTrue(ExpressionEvaluator.hasPendingOperation("15+"))
        assertTrue(ExpressionEvaluator.hasPendingOperation("20×"))
    }

    @Test
    fun `hasPendingOperation - 空字符串`() {
        assertFalse(ExpressionEvaluator.hasPendingOperation(""))
    }

    @Test
    fun `formatForDisplay - 基本格式化`() {
        assertEquals("15 + 20", ExpressionEvaluator.formatForDisplay("15+20"))
    }

    @Test
    fun `formatForDisplay - 连续运算`() {
        assertEquals("100 - 25.5 + 10", ExpressionEvaluator.formatForDisplay("100-25.5+10"))
    }

    @Test
    fun `formatForDisplay - 乘除运算`() {
        assertEquals("100 - 25 × 2", ExpressionEvaluator.formatForDisplay("100-25×2"))
    }

    @Test
    fun `formatForDisplay - 单个数字不变`() {
        assertEquals("15", ExpressionEvaluator.formatForDisplay("15"))
    }
}
