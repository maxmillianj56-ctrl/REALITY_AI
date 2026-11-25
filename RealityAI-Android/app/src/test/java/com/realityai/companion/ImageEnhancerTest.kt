package com.realityai.companion

import android.graphics.Color
import com.realityai.companion.domain.model.ImageAdjustmentParams
import com.realityai.companion.processing.ImageEnhancer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEnhancerTest {

    @Test
    fun parametersAreSafelyClamped() {
        val raw = ImageAdjustmentParams(
            brightness = 5.0f,
            contrast = -1.0f,
            saturation = 10.0f,
            sharpness = -2.0f,
            gamma = 9.0f
        ).clamped()

        assertEquals(0.5f, raw.brightness, 0.001f)
        assertEquals(0.5f, raw.contrast, 0.001f)
        assertEquals(2.0f, raw.saturation, 0.001f)
        assertEquals(0.0f, raw.sharpness, 0.001f)
        assertEquals(2.2f, raw.gamma, 0.001f)
    }

    @Test
    fun gammaLiftBrightensMidtonesWithoutOverflow() {
        val pixels = intArrayOf(Color.argb(255, 64, 64, 64))
        ImageEnhancer.applyGammaLutInPlace(pixels, 1.5f)
        val lifted = Color.red(pixels[0])
        assertTrue("Expected lifted midtone > 64, was $lifted", lifted > 64)
        assertTrue(lifted <= 255)
    }

    @Test
    fun sharpenKernelPreservesFlatRegion() {
        val flat = IntArray(9) { Color.argb(255, 100, 100, 100) }
        val out = ImageEnhancer.applySharpenKernel(flat, 3, 3, 1.0f)
        assertEquals(100, Color.red(out[4]))
        assertEquals(100, Color.green(out[4]))
        assertEquals(100, Color.blue(out[4]))
    }
}
