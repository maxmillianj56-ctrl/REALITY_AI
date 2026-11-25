package com.realityai.companion.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.realityai.companion.domain.model.ImageAdjustmentParams
import com.realityai.companion.domain.model.ProcessingQuality
import kotlin.math.pow
import kotlin.math.roundToInt

object ImageEnhancer {

    /**
     * Applies offline visual adjustments (brightness, contrast, saturation, gamma LUT,
     * and optional 3x3 unsharp-mask convolution) to a captured preview Bitmap.
     * Note: This strictly transforms the local preview Bitmap inside REALITY AI and never
     * alters any external game's rendering pipeline.
     */
    fun processPreview(
        source: Bitmap,
        params: ImageAdjustmentParams,
        quality: ProcessingQuality
    ): Bitmap {
        val clamped = params.clamped()
        val targetW = (source.width * quality.scaleFactor).roundToInt().coerceAtLeast(16)
        val targetH = (source.height * quality.scaleFactor).roundToInt().coerceAtLeast(16)

        val working = if (targetW != source.width || targetH != source.height) {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Step 1: Hardware-accelerated ColorMatrix pass for saturation, contrast, brightness
        val colorAdjusted = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(colorAdjusted)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildColorMatrix(clamped))
        }
        canvas.drawBitmap(working, 0f, 0f, paint)
        if (working !== source) {
            working.recycle()
        }

        if (quality == ProcessingQuality.LOW) {
            return colorAdjusted
        }

        val width = colorAdjusted.width
        val height = colorAdjusted.height
        val pixels = IntArray(width * height)
        colorAdjusted.getPixels(pixels, 0, width, 0, 0, width, height)

        // Step 2: Gamma LUT pass (runs on MEDIUM and HIGH)
        if (kotlin.math.abs(clamped.gamma - 1.0f) > 0.02f) {
            applyGammaLutInPlace(pixels, clamped.gamma)
        }

        // Step 3: 3x3 Unsharp-mask sharpening pass (runs on HIGH when sharpness > 0.05f)
        val finalPixels = if (quality == ProcessingQuality.HIGH && clamped.sharpness > 0.05f) {
            applySharpenKernel(pixels, width, height, clamped.sharpness)
        } else {
            pixels
        }

        colorAdjusted.setPixels(finalPixels, 0, width, 0, 0, width, height)
        return colorAdjusted
    }

    fun buildColorMatrix(params: ImageAdjustmentParams): ColorMatrix {
        val satMatrix = ColorMatrix().apply { setSaturation(params.saturation) }

        val c = params.contrast
        val t = (1f - c) * 127.5f + params.brightness * 255f
        val bcMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        bcMatrix.preConcat(satMatrix)
        return bcMatrix
    }

    internal fun applyGammaLutInPlace(pixels: IntArray, gamma: Float) {
        val invGamma = 1.0 / gamma.coerceIn(0.5f, 2.2f).toDouble()
        val lut = IntArray(256) { i ->
            (255.0 * (i / 255.0).pow(invGamma)).roundToInt().coerceIn(0, 255)
        }
        for (i in pixels.indices) {
            val px = pixels[i]
            val a = px ushr 24
            val r = lut[(px ushr 16) and 0xFF]
            val g = lut[(px ushr 8) and 0xFF]
            val b = lut[px and 0xFF]
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    internal fun applySharpenKernel(
        src: IntArray,
        width: Int,
        height: Int,
        amount: Float
    ): IntArray {
        val out = src.copyOf()
        val k = amount.coerceIn(0f, 1.5f)
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val c = src[idx]
                val n = src[idx - width]
                val s = src[idx + width]
                val w = src[idx - 1]
                val e = src[idx + 1]

                val a = c ushr 24
                val cr = (c ushr 16) and 0xFF
                val cg = (c ushr 8) and 0xFF
                val cb = c and 0xFF

                val lapR = 4 * cr - ((n ushr 16) and 0xFF) - ((s ushr 16) and 0xFF) -
                    ((w ushr 16) and 0xFF) - ((e ushr 16) and 0xFF)
                val lapG = 4 * cg - ((n ushr 8) and 0xFF) - ((s ushr 8) and 0xFF) -
                    ((w ushr 8) and 0xFF) - ((e ushr 8) and 0xFF)
                val lapB = 4 * cb - (n and 0xFF) - (s and 0xFF) - (w and 0xFF) - (e and 0xFF)

                val nr = (cr + k * lapR).roundToInt().coerceIn(0, 255)
                val ng = (cg + k * lapG).roundToInt().coerceIn(0, 255)
                val nb = (cb + k * lapB).roundToInt().coerceIn(0, 255)

                out[idx] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return out
    }
}
