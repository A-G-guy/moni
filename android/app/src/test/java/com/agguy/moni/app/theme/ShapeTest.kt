package com.agguy.moni.app.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shape.kt 单元测试。
 *
 * 验证 MoniShapes 各档位为预期的 [RoundedCornerShape]，且角半径符合
 * Material 3 Expressive 2025 规格。
 */
class ShapeTest {

    @Test
    fun `all shape slots are RoundedCornerShape`() {
        assertTrue(MoniShapes.extraSmall is RoundedCornerShape)
        assertTrue(MoniShapes.small is RoundedCornerShape)
        assertTrue(MoniShapes.medium is RoundedCornerShape)
        assertTrue(MoniShapes.large is RoundedCornerShape)
        assertTrue(MoniShapes.extraLarge is RoundedCornerShape)
        assertTrue(MoniShapes.largeIncreased is RoundedCornerShape)
        assertTrue(MoniShapes.extraLargeIncreased is RoundedCornerShape)
        assertTrue(MoniShapes.extraExtraLarge is RoundedCornerShape)
    }

    @Test
    fun `corner values match expressive spec`() {
        // RoundedCornerShape(Dp) 会生成内部 DpCornerSize；
        // 这里通过 isSameCorner 做弱等值断言（Compose 内部 toString 输出含 px 值，不可靠，
        // 故直接构造对照对象比较类行为）。
        assertShapesEqual(
            RoundedCornerShape(4.dp),
            MoniShapes.extraSmall,
            "extraSmall should be 4dp"
        )
        assertShapesEqual(
            RoundedCornerShape(8.dp),
            MoniShapes.small,
            "small should be 8dp"
        )
        assertShapesEqual(
            RoundedCornerShape(12.dp),
            MoniShapes.medium,
            "medium should be 12dp"
        )
        assertShapesEqual(
            RoundedCornerShape(20.dp),
            MoniShapes.large,
            "large should be 20dp (expressive)"
        )
        assertShapesEqual(
            RoundedCornerShape(32.dp),
            MoniShapes.extraLarge,
            "extraLarge should be 32dp (expressive)"
        )
        assertShapesEqual(
            RoundedCornerShape(24.dp),
            MoniShapes.largeIncreased,
            "largeIncreased should be 24dp"
        )
        assertShapesEqual(
            RoundedCornerShape(40.dp),
            MoniShapes.extraLargeIncreased,
            "extraLargeIncreased should be 40dp"
        )
        assertShapesEqual(
            RoundedCornerShape(48.dp),
            MoniShapes.extraExtraLarge,
            "extraExtraLarge should be 48dp (expressive)"
        )
    }

    private fun assertShapesEqual(expected: CornerBasedShape, actual: CornerBasedShape, msg: String) {
        // RoundedCornerShape 是 data class，直接比较会按四个 corner size 比较；
        // 当用单个 Dp 构造时，四个 corner 相等。
        assertTrue(msg, expected == actual)
    }
}
