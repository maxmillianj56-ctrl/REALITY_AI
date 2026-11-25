package com.realityai.companion.domain.model

enum class SceneCondition(val label: String) {
    DARK_LOW_VISIBILITY("Dark / Low-Visibility Interior"),
    BRIGHT_OVEREXPOSED("Bright / High-Glare Scene"),
    OUTDOOR_HIGH_CONTRAST("Outdoor / High-Contrast Environment"),
    VIVID_HUD_DENSE("Action / High Edge & HUD Density"),
    BALANCED_SCENE("Balanced Exposure Scene")
}

data class SceneAnalysisReport(
    val condition: SceneCondition,
    val meanLuminance: Float,
    val shadowClipRatio: Float,
    val highlightClipRatio: Float,
    val meanSaturation: Float,
    val edgeDensity: Float,
    val recommendedProfile: GraphicsProfileType,
    val recommendedAdjustments: ImageAdjustmentParams,
    val explanation: String,
    val analyzedAtMs: Long
)
