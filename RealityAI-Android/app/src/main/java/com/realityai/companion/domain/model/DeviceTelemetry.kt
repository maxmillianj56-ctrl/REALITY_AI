package com.realityai.companion.domain.model

enum class ThermalLevel(val label: String, val isThrottled: Boolean) {
    NONE("Nominal", false),
    LIGHT("Warm (Light)", false),
    MODERATE("Elevated (Moderate)", true),
    SEVERE("Hot (Severe)", true),
    CRITICAL("Critical", true),
    UNKNOWN("Unavailable", false)
}

data class DeviceTelemetry(
    val deviceModel: String = "",
    val hardwareBoard: String = "",
    val androidVersion: String = "",
    val isNote20UltraTarget: Boolean = false,
    val batteryPercent: Int? = null,
    val batteryTempCelsius: Float? = null,
    val isCharging: Boolean = false,
    val thermalLevel: ThermalLevel = ThermalLevel.UNKNOWN,
    val availableMemoryMb: Long? = null,
    val totalMemoryMb: Long? = null,
    val lowMemory: Boolean = false,
    val measuredPreviewFps: Float? = null,
    val targetPreviewFps: Int = 30,
    val effectivePreviewFpsCap: Int = 30,
    val thermalDownscaleReason: String? = null
)
