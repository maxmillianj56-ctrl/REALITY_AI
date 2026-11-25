package com.realityai.companion.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CaptureResolution(val label: String, val shortEdgePx: Int) {
    P540("540p (qHD)", 540),
    P720("720p (HD)", 720),
    P1080("1080p (FHD)", 1080)
}

@Serializable
enum class ProcessingQuality(val label: String, val description: String, val scaleFactor: Float) {
    LOW("Low", "Color matrix pass only; lowest CPU & battery draw", 0.5f),
    MEDIUM("Medium", "Color matrix + gamma LUT; balanced speed", 0.75f),
    HIGH("High", "Full resolution + gamma LUT + 3x3 sharpening kernel", 1.0f)
}

@Serializable
data class AppSettings(
    val captureResolution: CaptureResolution = CaptureResolution.P720,
    val processingQuality: ProcessingQuality = ProcessingQuality.MEDIUM,
    val batterySavingMode: Boolean = false,
    val autoDetectGames: Boolean = false,
    val thermalAutoDownscale: Boolean = true,
    val targetPreviewFps: Int = 30
)
