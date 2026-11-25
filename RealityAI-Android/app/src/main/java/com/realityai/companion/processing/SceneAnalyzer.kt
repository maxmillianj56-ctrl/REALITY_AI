package com.realityai.companion.processing

import android.graphics.Bitmap
import com.realityai.companion.domain.model.GraphicsProfileType
import com.realityai.companion.domain.model.ImageAdjustmentParams
import com.realityai.companion.domain.model.SceneAnalysisReport
import com.realityai.companion.domain.model.SceneCondition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SceneAnalyzer {

    /**
     * On-demand offline scene classifier. Runs ONLY when explicitly requested by the user
     * on a single captured frame to avoid continuous battery/CPU drain.
     */
    fun analyzeBitmap(bitmap: Bitmap): SceneAnalysisReport {
        val sampleW = 160
        val sampleH = ((bitmap.height.toFloat() / bitmap.width.coerceAtLeast(1)) * sampleW)
            .toInt().coerceIn(45, 160)
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return analyzePixelBuffer(pixels, sampleW, sampleH)
    }

    fun analyzePixelBuffer(pixels: IntArray, width: Int, height: Int): SceneAnalysisReport {
        if (pixels.isEmpty()) {
            return SceneAnalysisReport(
                condition = SceneCondition.BALANCED_SCENE,
                meanLuminance = 0.5f,
                shadowClipRatio = 0f,
                highlightClipRatio = 0f,
                meanSaturation = 0.4f,
                edgeDensity = 0.1f,
                recommendedProfile = GraphicsProfileType.BALANCED,
                recommendedAdjustments = GraphicsProfileType.BALANCED.defaultAdjustments,
                explanation = "No pixel data available; defaulting to BALANCED preset.",
                analyzedAtMs = System.currentTimeMillis()
            )
        }

        var lumSum = 0.0
        var satSum = 0.0
        var shadowCount = 0
        var highlightCount = 0
        val lums = FloatArray(pixels.size)

        for (i in pixels.indices) {
            val px = pixels[i]
            val r = ((px ushr 16) and 0xFF) / 255f
            val g = ((px ushr 8) and 0xFF) / 255f
            val b = (px and 0xFF) / 255f

            // Rec.709 relative luminance
            val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
            lums[i] = lum
            lumSum += lum
            if (lum < 0.14f) shadowCount++
            if (lum > 0.86f) highlightCount++

            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val sat = if (maxC > 0.001f) (maxC - minC) / maxC else 0f
            satSum += sat
        }

        var edgeSum = 0.0
        var edgeSamples = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val gx = abs(lums[row + x + 1] - lums[row + x - 1])
                val gy = abs(lums[row + x + width] - lums[row + x - width])
                if (gx + gy > 0.16f) edgeSum += 1.0
                edgeSamples++
            }
        }

        val count = pixels.size.toFloat()
        val meanLum = (lumSum / count).toFloat()
        val shadowRatio = shadowCount / count
        val highlightRatio = highlightCount / count
        val meanSat = (satSum / count).toFloat()
        val edgeDensity = if (edgeSamples > 0) (edgeSum / edgeSamples).toFloat() else 0f

        return classifyMetrics(
            meanLum = meanLum,
            shadowRatio = shadowRatio,
            highlightRatio = highlightRatio,
            meanSat = meanSat,
            edgeDensity = edgeDensity
        )
    }

    private fun classifyMetrics(
        meanLum: Float,
        shadowRatio: Float,
        highlightRatio: Float,
        meanSat: Float,
        edgeDensity: Float
    ): SceneAnalysisReport {
        val now = System.currentTimeMillis()
        return when {
            meanLum < 0.28f || shadowRatio > 0.48f -> {
                SceneAnalysisReport(
                    condition = SceneCondition.DARK_LOW_VISIBILITY,
                    meanLuminance = meanLum,
                    shadowClipRatio = shadowRatio,
                    highlightClipRatio = highlightRatio,
                    meanSaturation = meanSat,
                    edgeDensity = edgeDensity,
                    recommendedProfile = GraphicsProfileType.CINEMATIC,
                    recommendedAdjustments = ImageAdjustmentParams(
                        brightness = 0.10f,
                        contrast = 1.12f,
                        saturation = 1.08f,
                        sharpness = 0.35f,
                        gamma = 1.24f
                    ),
                    explanation = "Detected a dark scene (${(meanLum * 100).toInt()}% mean luminance, ${(shadowRatio * 100).toInt()}% deep shadows). Raising gamma to 1.24 and brightness (+0.10) opens shadow detail for preview analysis without washing out midtones.",
                    analyzedAtMs = now
                )
            }
            meanLum > 0.72f || highlightRatio > 0.35f -> {
                SceneAnalysisReport(
                    condition = SceneCondition.BRIGHT_OVEREXPOSED,
                    meanLuminance = meanLum,
                    shadowClipRatio = shadowRatio,
                    highlightClipRatio = highlightRatio,
                    meanSaturation = meanSat,
                    edgeDensity = edgeDensity,
                    recommendedProfile = GraphicsProfileType.BALANCED,
                    recommendedAdjustments = ImageAdjustmentParams(
                        brightness = -0.07f,
                        contrast = 1.15f,
                        saturation = 1.06f,
                        sharpness = 0.25f,
                        gamma = 0.88f
                    ),
                    explanation = "Detected bright/high-glare highlights (${(highlightRatio * 100).toInt()}% near-white pixels). Lowering gamma to 0.88 and brightness (-0.07) recovers highlight separation in sunlit skies or bright terrain.",
                    analyzedAtMs = now
                )
            }
            shadowRatio > 0.20f && highlightRatio > 0.14f -> {
                SceneAnalysisReport(
                    condition = SceneCondition.OUTDOOR_HIGH_CONTRAST,
                    meanLuminance = meanLum,
                    shadowClipRatio = shadowRatio,
                    highlightClipRatio = highlightRatio,
                    meanSaturation = meanSat,
                    edgeDensity = edgeDensity,
                    recommendedProfile = GraphicsProfileType.ULTRA,
                    recommendedAdjustments = ImageAdjustmentParams(
                        brightness = 0.02f,
                        contrast = 1.06f,
                        saturation = 1.14f,
                        sharpness = 0.50f,
                        gamma = 1.08f
                    ),
                    explanation = "Detected an outdoor high-contrast split between bright sky/ground and hard shadows. Mild gamma lift (1.08) with 0.50 sharpening clarifies distant silhouettes.",
                    analyzedAtMs = now
                )
            }
            edgeDensity > 0.22f -> {
                SceneAnalysisReport(
                    condition = SceneCondition.VIVID_HUD_DENSE,
                    meanLuminance = meanLum,
                    shadowClipRatio = shadowRatio,
                    highlightClipRatio = highlightRatio,
                    meanSaturation = meanSat,
                    edgeDensity = edgeDensity,
                    recommendedProfile = GraphicsProfileType.PERFORMANCE,
                    recommendedAdjustments = ImageAdjustmentParams(
                        brightness = 0.02f,
                        contrast = 1.10f,
                        saturation = if (meanSat > 0.55f) 0.96f else 1.12f,
                        sharpness = 0.15f,
                        gamma = 1.02f
                    ),
                    explanation = "High edge density (${(edgeDensity * 100).toInt()}%) suggests fast action or dense HUD elements. Minimal sharpening avoids ringing artifacts while PERFORMANCE mode preserves thermal headroom.",
                    analyzedAtMs = now
                )
            }
            else -> {
                SceneAnalysisReport(
                    condition = SceneCondition.BALANCED_SCENE,
                    meanLuminance = meanLum,
                    shadowClipRatio = shadowRatio,
                    highlightClipRatio = highlightRatio,
                    meanSaturation = meanSat,
                    edgeDensity = edgeDensity,
                    recommendedProfile = GraphicsProfileType.BALANCED,
                    recommendedAdjustments = ImageAdjustmentParams(
                        brightness = 0.01f,
                        contrast = 1.12f,
                        saturation = 1.10f,
                        sharpness = 0.30f,
                        gamma = 1.04f
                    ),
                    explanation = "Frame exposure is well balanced (${(meanLum * 100).toInt()}% luminance, ${(meanSat * 100).toInt()}% saturation). BALANCED preset applies subtle contrast and clarity without clipping.",
                    analyzedAtMs = now
                )
            }
        }
    }
}
