package com.agguy.moni.app.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MoniCardVariant 枚举的单元测试。
 *
 * 覆盖范围：
 * - 枚举值数量与名称。
 * - valueOf 行为。
 * - values() 内容稳定。
 * - name 与 ordinal 行为。
 */
class MoniCardTest {

    @Test
    fun `MoniCardVariant 包含 4 个枚举值`() {
        assertEquals(4, MoniCardVariant.values().size)
    }

    @Test
    fun `MoniCardVariant values 内容稳定`() {
        val expected = arrayOf(
            MoniCardVariant.Filled,
            MoniCardVariant.Tonal,
            MoniCardVariant.Outlined,
            MoniCardVariant.Elevated
        )

        assertEquals(expected.toList(), MoniCardVariant.values().toList())
    }

    @Test
    fun `MoniCardVariant valueOf 正确解析每个名称`() {
        assertEquals(MoniCardVariant.Filled, MoniCardVariant.valueOf("Filled"))
        assertEquals(MoniCardVariant.Tonal, MoniCardVariant.valueOf("Tonal"))
        assertEquals(MoniCardVariant.Outlined, MoniCardVariant.valueOf("Outlined"))
        assertEquals(MoniCardVariant.Elevated, MoniCardVariant.valueOf("Elevated"))
    }

    @Test
    fun `MoniCardVariant name 与 ordinal 行为`() {
        assertEquals("Filled", MoniCardVariant.Filled.name)
        assertEquals(0, MoniCardVariant.Filled.ordinal)

        assertEquals("Tonal", MoniCardVariant.Tonal.name)
        assertEquals(1, MoniCardVariant.Tonal.ordinal)

        assertEquals("Outlined", MoniCardVariant.Outlined.name)
        assertEquals(2, MoniCardVariant.Outlined.ordinal)

        assertEquals("Elevated", MoniCardVariant.Elevated.name)
        assertEquals(3, MoniCardVariant.Elevated.ordinal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `MoniCardVariant valueOf 非法名称抛出异常`() {
        MoniCardVariant.valueOf("Unknown")
    }
}
