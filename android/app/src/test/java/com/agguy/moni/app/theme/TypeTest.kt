package com.agguy.moni.app.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Type.kt 单元测试。
 *
 * 验证标准 15 个槽位与 expressive emphasized 变体均正确初始化。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class TypeTest {

    @Test
    fun `standard slots are not null`() {
        val t = Typography
        assertNotNull("displayLarge", t.displayLarge)
        assertNotNull("displayMedium", t.displayMedium)
        assertNotNull("displaySmall", t.displaySmall)
        assertNotNull("headlineLarge", t.headlineLarge)
        assertNotNull("headlineMedium", t.headlineMedium)
        assertNotNull("headlineSmall", t.headlineSmall)
        assertNotNull("titleLarge", t.titleLarge)
        assertNotNull("titleMedium", t.titleMedium)
        assertNotNull("titleSmall", t.titleSmall)
        assertNotNull("bodyLarge", t.bodyLarge)
        assertNotNull("bodyMedium", t.bodyMedium)
        assertNotNull("bodySmall", t.bodySmall)
        assertNotNull("labelLarge", t.labelLarge)
        assertNotNull("labelMedium", t.labelMedium)
        assertNotNull("labelSmall", t.labelSmall)
    }

    @Test
    fun `all emphasized variants are not null`() {
        val t = Typography
        assertNotNull("displayLargeEmphasized", t.displayLargeEmphasized)
        assertNotNull("displayMediumEmphasized", t.displayMediumEmphasized)
        assertNotNull("displaySmallEmphasized", t.displaySmallEmphasized)
        assertNotNull("headlineLargeEmphasized", t.headlineLargeEmphasized)
        assertNotNull("headlineMediumEmphasized", t.headlineMediumEmphasized)
        assertNotNull("headlineSmallEmphasized", t.headlineSmallEmphasized)
        assertNotNull("titleLargeEmphasized", t.titleLargeEmphasized)
        assertNotNull("titleMediumEmphasized", t.titleMediumEmphasized)
        assertNotNull("titleSmallEmphasized", t.titleSmallEmphasized)
        assertNotNull("bodyLargeEmphasized", t.bodyLargeEmphasized)
        assertNotNull("bodyMediumEmphasized", t.bodyMediumEmphasized)
        assertNotNull("bodySmallEmphasized", t.bodySmallEmphasized)
        assertNotNull("labelLargeEmphasized", t.labelLargeEmphasized)
        assertNotNull("labelMediumEmphasized", t.labelMediumEmphasized)
        assertNotNull("labelSmallEmphasized", t.labelSmallEmphasized)
    }

    @Test
    fun `emphasized variants have heavier font weight than base`() {
        val t = Typography
        assertEquals(FontWeight.ExtraBold, t.displayLargeEmphasized.fontWeight)
        assertEquals(FontWeight.ExtraBold, t.displayMediumEmphasized.fontWeight)
        assertEquals(FontWeight.ExtraBold, t.displaySmallEmphasized.fontWeight)
        assertEquals(FontWeight.ExtraBold, t.headlineLargeEmphasized.fontWeight)
        assertEquals(FontWeight.Bold, t.headlineMediumEmphasized.fontWeight)
        assertEquals(FontWeight.Bold, t.headlineSmallEmphasized.fontWeight)
        assertEquals(FontWeight.Bold, t.titleLargeEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.titleMediumEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.titleSmallEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.bodyLargeEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.bodyMediumEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.bodySmallEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.labelLargeEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.labelMediumEmphasized.fontWeight)
        assertEquals(FontWeight.SemiBold, t.labelSmallEmphasized.fontWeight)
    }

    @Test
    fun `emphasized display sizes use tighter letter spacing`() {
        val t = Typography
        // expressive display 强调字号的 letterSpacing = (-0.25).sp
        assertEquals(-0.25f, t.displayLargeEmphasized.letterSpacing.value, 0.001f)
        assertEquals(-0.25f, t.displayMediumEmphasized.letterSpacing.value, 0.001f)
        assertEquals(-0.25f, t.displaySmallEmphasized.letterSpacing.value, 0.001f)
    }
}
