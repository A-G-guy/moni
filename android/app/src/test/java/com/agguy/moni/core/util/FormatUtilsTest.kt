package com.agguy.moni.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `format exact yuan`() {
        assertEquals("10", formatAmount(1000))
        assertEquals("0", formatAmount(0))
        assertEquals("999", formatAmount(99900))
    }

    @Test
    fun `format with fen`() {
        assertEquals("10.50", formatAmount(1050))
        assertEquals("0.01", formatAmount(1))
        assertEquals("0.99", formatAmount(99))
    }

    @Test
    fun `format negative amount`() {
        assertEquals("-10", formatAmount(-1000))
        assertEquals("-10.50", formatAmount(-1050))
        assertEquals("-0.01", formatAmount(-1))
    }

    @Test
    fun `format single digit fen`() {
        assertEquals("0.05", formatAmount(5))
        assertEquals("10.05", formatAmount(1005))
    }
}
