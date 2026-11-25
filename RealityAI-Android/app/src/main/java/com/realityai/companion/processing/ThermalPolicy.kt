package com.realityai.companion.processing

import com.realityai.companion.domain.model.AppSettings
import com.realityai.companion.domain.model.ProcessingQuality
import com.realityai.companion.domain.model.ThermalLevel

data class EffectiveProcessingBudget(
    val targetFpsCap: Int,
    val processingQuality: ProcessingQuality,
    val isDownscaled: Boolean,
    val reason: String?
)

object ThermalPolicy {
    fun computeEffectiveBudget(
        settings: AppSettings,
        thermalLevel: ThermalLevel,
        batteryTempCelsius: Float?
    ): EffectiveProcessingBudget {
        val baseCap = settings.targetPreviewFps.coerceIn(15, 30)

        if (settings.batterySavingMode) {
            return EffectiveProcessingBudget(
                targetFpsCap = 15,
                processingQuality = ProcessingQuality.LOW,
                isDownscaled = true,
                reason = "Battery-saving mode enabled (15 FPS preview cap, Low quality pass)"
            )
        }

        if (!settings.thermalAutoDownscale) {
            return EffectiveProcessingBudget(
                targetFpsCap = baseCap,
                processingQuality = settings.processingQuality,
                isDownscaled = false,
                reason = null
            )
        }

        val tempHot = (batteryTempCelsius ?: 0f) >= 42.5f
        val tempWarm = (batteryTempCelsius ?: 0f) >= 39.5f

        return when {
            thermalLevel == ThermalLevel.SEVERE || thermalLevel == ThermalLevel.CRITICAL || tempHot -> {
                EffectiveProcessingBudget(
                    targetFpsCap = 15,
                    processingQuality = ProcessingQuality.LOW,
                    isDownscaled = true,
                    reason = "Thermal protection active (${thermalLevel.label}) — lowered preview to 15 FPS / Low quality"
                )
            }
            thermalLevel == ThermalLevel.MODERATE || tempWarm -> {
                val steppedQuality = when (settings.processingQuality) {
                    ProcessingQuality.HIGH -> ProcessingQuality.MEDIUM
                    ProcessingQuality.MEDIUM -> ProcessingQuality.LOW
                    ProcessingQuality.LOW -> ProcessingQuality.LOW
                }
                EffectiveProcessingBudget(
                    targetFpsCap = 20,
                    processingQuality = steppedQuality,
                    isDownscaled = true,
                    reason = "Warm device load — stepped preview down to 20 FPS / ${steppedQuality.label} quality"
                )
            }
            else -> EffectiveProcessingBudget(
                targetFpsCap = baseCap,
                processingQuality = settings.processingQuality,
                isDownscaled = false,
                reason = null
            )
        }
    }
}
