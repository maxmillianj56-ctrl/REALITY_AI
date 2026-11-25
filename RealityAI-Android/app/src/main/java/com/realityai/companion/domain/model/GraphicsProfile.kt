package com.realityai.companion.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GraphicsProfileType(
    val displayName: String,
    val tagline: String,
    val description: String,
    val targetPreviewFps: Int,
    val recommendedResolution: CaptureResolution,
    val recommendedQuality: ProcessingQuality,
    val defaultAdjustments: ImageAdjustmentParams
) {
    PERFORMANCE(
        displayName = "PERFORMANCE",
        tagline = "Low-overhead 540p analysis & fast frame cadence",
        description = "Minimizes CPU/memory footprint during MediaProjection capture. Skips expensive sharpening convolution and runs at 540p so thermal headroom stays reserved for your game.",
        targetPreviewFps = 30,
        recommendedResolution = CaptureResolution.P540,
        recommendedQuality = ProcessingQuality.LOW,
        defaultAdjustments = ImageAdjustmentParams(
            brightness = 0.04f,
            contrast = 1.08f,
            saturation = 1.05f,
            sharpness = 0.0f,
            gamma = 1.0f
        )
    ),
    BALANCED(
        displayName = "BALANCED",
        tagline = "720p clarity with thermal-safe preview tuning",
        description = "Default Note 20 Ultra profile. Balances 720p capture clarity with moderate contrast and gamma calibration while stepping down automatically under thermal load.",
        targetPreviewFps = 30,
        recommendedResolution = CaptureResolution.P720,
        recommendedQuality = ProcessingQuality.MEDIUM,
        defaultAdjustments = ImageAdjustmentParams(
            brightness = 0.02f,
            contrast = 1.12f,
            saturation = 1.10f,
            sharpness = 0.25f,
            gamma = 1.05f
        )
    ),
    CINEMATIC(
        displayName = "CINEMATIC",
        tagline = "Rich tonal depth & filmic shadow contrast",
        description = "Tuned for dark atmospheric RPGs and story titles. Lifts shadow legibility slightly while preserving deep charcoal blacks and warm-to-neutral saturation.",
        targetPreviewFps = 24,
        recommendedResolution = CaptureResolution.P720,
        recommendedQuality = ProcessingQuality.HIGH,
        defaultAdjustments = ImageAdjustmentParams(
            brightness = 0.05f,
            contrast = 1.18f,
            saturation = 1.16f,
            sharpness = 0.35f,
            gamma = 1.12f
        )
    ),
    ULTRA(
        displayName = "ULTRA",
        tagline = "Full 1080p capture with unsharp detail pass",
        description = "Maximum preview fidelity for screenshot inspection and visual comparison. Uses 1080p MediaProjection frames and full gamma LUT + sharpening convolution on-device.",
        targetPreviewFps = 30,
        recommendedResolution = CaptureResolution.P1080,
        recommendedQuality = ProcessingQuality.HIGH,
        defaultAdjustments = ImageAdjustmentParams(
            brightness = 0.0f,
            contrast = 1.22f,
            saturation = 1.18f,
            sharpness = 0.65f,
            gamma = 1.04f
        )
    )
}

@Serializable
data class ImageAdjustmentParams(
    val brightness: Float = 0f,     // -0.50f .. +0.50f
    val contrast: Float = 1.0f,     //  0.50f ..  2.00f
    val saturation: Float = 1.0f,   //  0.00f ..  2.00f
    val sharpness: Float = 0.0f,    //  0.00f ..  1.50f
    val gamma: Float = 1.0f         //  0.50f ..  2.20f
) {
    fun clamped(): ImageAdjustmentParams = ImageAdjustmentParams(
        brightness = brightness.coerceIn(-0.5f, 0.5f),
        contrast = contrast.coerceIn(0.5f, 2.0f),
        saturation = saturation.coerceIn(0.0f, 2.0f),
        sharpness = sharpness.coerceIn(0.0f, 1.5f),
        gamma = gamma.coerceIn(0.5f, 2.2f)
    )
}
