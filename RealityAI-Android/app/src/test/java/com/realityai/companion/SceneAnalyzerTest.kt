package com.realityai.companion

import android.graphics.Color
import com.realityai.companion.domain.model.GraphicsProfileType
import com.realityai.companion.domain.model.SceneCondition
import com.realityai.companion.processing.SceneAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneAnalyzerTest {

    @Test
    fun darkSceneRecommendsCinematicGammaLift() {
        val pixels = IntArray(40 * 30) { Color.rgb(18, 20, 24) }
        val report = SceneAnalyzer.analyzePixelBuffer(pixels, 40, 30)

        assertEquals(SceneCondition.DARK_LOW_VISIBILITY, report.condition)
        assertEquals(GraphicsProfileType.CINEMATIC, report.recommendedProfile)
        assertTrue(report.recommendedAdjustments.gamma > 1.1f)
        assertTrue(report.recommendedAdjustments.brightness > 0f)
    }

    @Test
    fun brightSceneRecommendsHighlightRecovery() {
        val pixels = IntArray(40 * 30) { Color.rgb(245, 242, 236) }
        val report = SceneAnalyzer.analyzePixelBuffer(pixels, 40, 30)

        assertEquals(SceneCondition.BRIGHT_OVEREXPOSED, report.condition)
        assertTrue(report.recommendedAdjustments.gamma < 1.0f)
        assertTrue(report.recommendedAdjustments.brightness < 0f)
    }

    @Test
    fun outdoorHighContrastSplitIsDetected() {
        val w = 40
        val h = 30
        val pixels = IntArray(w * h) { idx ->
            val y = idx / w
            if (y < h / 2) Color.rgb(242, 245, 250) else Color.rgb(20, 22, 24)
        }
        val report = SceneAnalyzer.analyzePixelBuffer(pixels, w, h)

        assertEquals(SceneCondition.OUTDOOR_HIGH_CONTRAST, report.condition)
        assertEquals(GraphicsProfileType.ULTRA, report.recommendedProfile)
    }
}
