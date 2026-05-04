package com.agguy.moni.app.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * MotionScheme 单元测试。
 *
 * 验证 Expressive motion scheme 的 6 条 spec 均返回有效的 [FiniteAnimationSpec]。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MotionTest {

    @Test
    fun `expressive motion scheme provides all specs`() {
        val scheme = MotionScheme.expressive()

        val spatialFloat: FiniteAnimationSpec<Float> = scheme.defaultSpatialSpec()
        val fastSpatialFloat: FiniteAnimationSpec<Float> = scheme.fastSpatialSpec()
        val slowSpatialFloat: FiniteAnimationSpec<Float> = scheme.slowSpatialSpec()

        val effectsFloat: FiniteAnimationSpec<Float> = scheme.defaultEffectsSpec()
        val fastEffectsFloat: FiniteAnimationSpec<Float> = scheme.fastEffectsSpec()
        val slowEffectsFloat: FiniteAnimationSpec<Float> = scheme.slowEffectsSpec()

        assertNotNull("defaultSpatialSpec", spatialFloat)
        assertNotNull("fastSpatialSpec", fastSpatialFloat)
        assertNotNull("slowSpatialSpec", slowSpatialFloat)
        assertNotNull("defaultEffectsSpec", effectsFloat)
        assertNotNull("fastEffectsSpec", fastEffectsFloat)
        assertNotNull("slowEffectsSpec", slowEffectsFloat)
    }

    @Test
    fun `spatial specs are non null`() {
        val scheme = MotionScheme.expressive()
        val spec: FiniteAnimationSpec<Float> = scheme.defaultSpatialSpec()
        assertNotNull("defaultSpatialSpec should not be null", spec)
    }

    @Test
    fun `specs accept different target types`() {
        val scheme = MotionScheme.expressive()
        val specSize: FiniteAnimationSpec<IntSize> = scheme.defaultSpatialSpec()
        val specOffset: FiniteAnimationSpec<IntOffset> = scheme.defaultSpatialSpec()
        assertNotNull("spatialSpec<IntSize>", specSize)
        assertNotNull("spatialSpec<IntOffset>", specOffset)
    }
}
