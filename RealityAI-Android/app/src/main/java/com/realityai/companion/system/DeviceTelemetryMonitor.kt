package com.realityai.companion.system

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.realityai.companion.domain.model.DeviceTelemetry
import com.realityai.companion.domain.model.ThermalLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeviceTelemetryMonitor(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _telemetry = MutableStateFlow(buildInitialTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private var registered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            updateFromBatteryIntent(intent)
        }
    }

    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        _telemetry.update { it.copy(thermalLevel = mapThermalStatus(status)) }
    }

    private fun buildInitialTelemetry(): DeviceTelemetry {
        val model = Build.MODEL.orEmpty()
        val isNote20Ultra = model.contains("SM-N986", ignoreCase = true) ||
            Build.DEVICE.contains("c2q", ignoreCase = true)
        return DeviceTelemetry(
            deviceModel = "${Build.MANUFACTURER} $model".trim(),
            hardwareBoard = Build.HARDWARE.orEmpty(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            isNote20UltraTarget = isNote20Ultra,
            thermalLevel = powerManager?.currentThermalStatus?.let(::mapThermalStatus)
                ?: ThermalLevel.UNKNOWN
        )
    }

    fun start() {
        if (registered) return
        registered = true
        val stickyIntent = context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (stickyIntent != null) {
            updateFromBatteryIntent(stickyIntent)
        }
        powerManager?.addThermalStatusListener(thermalListener)
        refreshMemoryAndThermal()
    }

    fun stop() {
        if (!registered) return
        registered = false
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { powerManager?.removeThermalStatusListener(thermalListener) }
    }

    fun refreshMemoryAndThermal() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val availMb = if (memInfo.totalMem > 0L) memInfo.availMem / (1024L * 1024L) else null
        val totalMb = if (memInfo.totalMem > 0L) memInfo.totalMem / (1024L * 1024L) else null
        val thermal = powerManager?.currentThermalStatus?.let(::mapThermalStatus) ?: ThermalLevel.UNKNOWN

        _telemetry.update {
            it.copy(
                availableMemoryMb = availMb,
                totalMemoryMb = totalMb,
                lowMemory = memInfo.lowMemory,
                thermalLevel = thermal
            )
        }
    }

    private fun updateFromBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else null
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tempTenths != Int.MIN_VALUE && tempTenths > -200) tempTenths / 10f else null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        _telemetry.update {
            it.copy(
                batteryPercent = pct,
                batteryTempCelsius = tempC,
                isCharging = charging
            )
        }
    }

    private fun mapThermalStatus(status: Int): ThermalLevel = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalLevel.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL,
        PowerManager.THERMAL_STATUS_EMERGENCY,
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.CRITICAL
        else -> ThermalLevel.UNKNOWN
    }
}
