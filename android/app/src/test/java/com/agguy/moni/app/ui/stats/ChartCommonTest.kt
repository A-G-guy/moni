package com.agguy.moni.app.ui.stats

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * ChartCommon 中纯函数与数据类的单元测试。
 *
 * 覆盖范围：
 * - formatShortAmount：小额、千元、万元、负数、零、极大值边界。
 * - PieColors：长度与颜色非透明断言。
 * - pieColorAt：取模循环行为，边界索引。
 * - LegendItem：data class equals / copy。
 */
class ChartCommonTest {

    @Test
    fun `formatShortAmount 零值输出仅符号与零`() {
        assertEquals("¥0", formatShortAmount(0L, "¥"))
    }

    @Test
    fun `formatShortAmount 小额保留原始元值`() {
        assertEquals("¥1", formatShortAmount(100L, "¥"))
        assertEquals("¥9", formatShortAmount(900L, "¥"))
        assertEquals("¥99", formatShortAmount(9_900L, "¥"))
        assertEquals("¥999", formatShortAmount(99_900L, "¥"))
    }

    @Test
    fun `formatShortAmount 千元以 k 后缀输出`() {
        assertEquals("¥1k", formatShortAmount(100_000L, "¥"))
        assertEquals("¥5k", formatShortAmount(500_000L, "¥"))
        assertEquals("¥9k", formatShortAmount(999_900L, "¥"))
    }

    @Test
    fun `formatShortAmount 万元以 w 后缀输出`() {
        assertEquals("¥1w", formatShortAmount(1_000_000L, "¥"))
        assertEquals("¥10w", formatShortAmount(10_000_000L, "¥"))
        assertEquals("¥99w", formatShortAmount(99_900_000L, "¥"))
    }

    @Test
    fun `formatShortAmount 负数直接输出元值不做缩写`() {
        // 负数 yuan 始终小于正阈值，故不会进入 k/w 分支
        assertEquals("¥-1", formatShortAmount(-100L, "¥"))
        assertEquals("¥-1000", formatShortAmount(-100_000L, "¥"))
        assertEquals("¥-10000", formatShortAmount(-1_000_000L, "¥"))
    }

    @Test
    fun `formatShortAmount 极大值正确截断`() {
        assertEquals("¥1000w", formatShortAmount(1_000_000_000L, "¥"))
    }

    @Test
    fun `formatShortAmount 支持自定义货币符号`() {
        assertEquals("$5k", formatShortAmount(500_000L, "$"))
        assertEquals("€1w", formatShortAmount(1_000_000L, "€"))
    }

    @Test
    fun `PieColors 长度固定为 12`() {
        assertEquals(12, PieColors.size)
    }

    @Test
    fun `PieColors 所有颜色均不透明`() {
        PieColors.forEachIndexed { index, color ->
            assertEquals(
                "颜色 $index 应完全不透明",
                1.0f,
                color.alpha,
                0f
            )
        }
    }

    @Test
    fun `pieColorAt 索引 0 返回第一个颜色`() {
        assertEquals(PieColors[0], pieColorAt(0))
    }

    @Test
    fun `pieColorAt 最后一个有效索引返回最后一个颜色`() {
        assertEquals(PieColors[PieColors.size - 1], pieColorAt(PieColors.size - 1))
    }

    @Test
    fun `pieColorAt 索引等于长度时循环到第一个颜色`() {
        assertEquals(PieColors[0], pieColorAt(PieColors.size))
    }

    @Test
    fun `pieColorAt 索引为长度两倍时循环到第一个颜色`() {
        assertEquals(PieColors[0], pieColorAt(PieColors.size * 2))
    }

    @Test(expected = ArrayIndexOutOfBoundsException::class)
    fun `pieColorAt 负索引抛出越界异常`() {
        pieColorAt(-1)
    }

    @Test
    fun `LegendItem equals 与 copy 行为`() {
        val a = LegendItem(label = "餐饮", color = Color.Red)
        val b = LegendItem(label = "餐饮", color = Color.Red)
        val c = LegendItem(label = "交通", color = Color.Blue)

        assertEquals(a, b)
        assertNotEquals(a, c)

        val copied = a.copy(label = "购物")
        assertEquals("购物", copied.label)
        assertEquals(Color.Red, copied.color)
        assertNotEquals(a, copied)
    }
}
