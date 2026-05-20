package com.agguy.moni.app.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/** PresetColorScheme 仪器测试 — 需 Android 环境以支持 Compose 与资源解析。 */
class PresetColorSchemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun all_preset_schemes_have_non_null_light_color_scheme() {
        PresetColorScheme.entries.forEach { scheme ->
            val colorScheme = scheme.toLightColorScheme()
            assertNotNull("$scheme lightColorScheme should not be null", colorScheme)
        }
    }

    @Test
    fun all_preset_schemes_have_non_null_dark_color_scheme() {
        PresetColorScheme.entries.forEach { scheme ->
            val colorScheme = scheme.toDarkColorScheme()
            assertNotNull("$scheme darkColorScheme should not be null", colorScheme)
        }
    }

    @Test
    fun default_light_primary_matches_expected() {
        val scheme = PresetColorScheme.DEFAULT.toLightColorScheme()
        assertEquals(Color(0xFF6750A4), scheme.primary)
    }

    @Test
    fun airy_sakura_light_primary_matches_expected() {
        val scheme = PresetColorScheme.AIRY_SAKURA.toLightColorScheme()
        assertEquals(Color(0xFF9B3E64), scheme.primary)
    }

    @Test
    fun anime_sky_light_primary_matches_expected() {
        val scheme = PresetColorScheme.ANIME_SKY.toLightColorScheme()
        assertEquals(Color(0xFF0061A6), scheme.primary)
    }

    @Test
    fun crisp_mint_light_primary_matches_expected() {
        val scheme = PresetColorScheme.CRISP_MINT.toLightColorScheme()
        assertEquals(Color(0xFF006C4E), scheme.primary)
    }

    @Test
    fun neon_lavender_light_primary_matches_expected() {
        val scheme = PresetColorScheme.NEON_LAVENDER.toLightColorScheme()
        assertEquals(Color(0xFF7D2ECB), scheme.primary)
    }

    @Test
    fun oatmeal_gold_light_primary_matches_expected() {
        val scheme = PresetColorScheme.OATMEAL_GOLD.toLightColorScheme()
        assertEquals(Color(0xFF7D562D), scheme.primary)
    }

    @Test
    fun sunset_coral_light_primary_matches_expected() {
        val scheme = PresetColorScheme.SUNSET_CORAL.toLightColorScheme()
        assertEquals(Color(0xFFA43D0F), scheme.primary)
    }

    @Test
    fun display_names_are_in_chinese() {
        composeTestRule.setContent {
            assertEquals("默认", PresetColorScheme.DEFAULT.displayName())
            assertEquals("动态颜色", PresetColorScheme.DYNAMIC.displayName())
            assertEquals("晴空樱粉", PresetColorScheme.AIRY_SAKURA.displayName())
            assertEquals("动漫天蓝", PresetColorScheme.ANIME_SKY.displayName())
            assertEquals("清新薄荷", PresetColorScheme.CRISP_MINT.displayName())
            assertEquals("霓虹薰衣草", PresetColorScheme.NEON_LAVENDER.displayName())
            assertEquals("燕麦金", PresetColorScheme.OATMEAL_GOLD.displayName())
            assertEquals("落日珊瑚", PresetColorScheme.SUNSET_CORAL.displayName())
        }
    }

    @Test
    fun seed_colors_match_expected_values() {
        assertEquals(Color(0xFF6750A4), PresetColorScheme.DEFAULT.seedColor)
        assertEquals(Color(0xFF9C27B0), PresetColorScheme.DYNAMIC.seedColor)
        assertEquals(Color(0xFFFF8FB8), PresetColorScheme.AIRY_SAKURA.seedColor)
        assertEquals(Color(0xFF269BFF), PresetColorScheme.ANIME_SKY.seedColor)
        assertEquals(Color(0xFF17D19B), PresetColorScheme.CRISP_MINT.seedColor)
        assertEquals(Color(0xFF974DE6), PresetColorScheme.NEON_LAVENDER.seedColor)
        assertEquals(Color(0xFFD4A373), PresetColorScheme.OATMEAL_GOLD.seedColor)
        assertEquals(Color(0xFFFA7C4B), PresetColorScheme.SUNSET_CORAL.seedColor)
    }

    @Test
    fun dynamic_scheme_reports_is_dynamic() {
        assertEquals(true, PresetColorScheme.DYNAMIC.isDynamic())
    }

    @Test
    fun non_dynamic_schemes_report_not_dynamic() {
        assertEquals(false, PresetColorScheme.DEFAULT.isDynamic())
        assertEquals(false, PresetColorScheme.AIRY_SAKURA.isDynamic())
        assertEquals(false, PresetColorScheme.ANIME_SKY.isDynamic())
        assertEquals(false, PresetColorScheme.CRISP_MINT.isDynamic())
        assertEquals(false, PresetColorScheme.NEON_LAVENDER.isDynamic())
        assertEquals(false, PresetColorScheme.OATMEAL_GOLD.isDynamic())
        assertEquals(false, PresetColorScheme.SUNSET_CORAL.isDynamic())
    }
}