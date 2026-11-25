package com.realityai.companion

import com.realityai.companion.domain.model.AppSettings
import com.realityai.companion.domain.model.ProcessingQuality
import com.realityai.companion.domain.model.ThermalLevel
import com.realityai.companion.processing.ThermalPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPolicyTest {

    @Test
    fun nominalThermalKeeps30FpsCap() {
        val budget = ThermalPolicy.computeEffectiveBudget(
            settings = AppSettings(processingQuality = ProcessingQuality.HIGH),
            thermalLevel = ThermalLevel.NONE,
            batteryTempCelsius = 33.0f
        )
        assertEquals(30, budget.targetFpsCap)
        assertEquals(ProcessingQuality.HIGH, budget.processingQuality)
        assertFalse(budget.isDownscaled)
    }

    @Test
    fun severeThermalDownscalesTo15FpsLowQuality() {
        val budget = ThermalPolicy.computeEffectiveBudget(
            settings = AppSettings(processingQuality = ProcessingQuality.HIGH),
            thermalLevel = ThermalLevel.SEVERE,
            batteryTempCelsius = 43.2f
        )
        assertEquals(15, budget.targetFpsCap)
        assertEquals(ProcessingQuality.LOW, budget.processingQuality)
        assertTrue(budget.isDownscaled)
    }

    @Test
    fun batterySaverOverridesTo15FpsLow() {
        val budget = ThermalPolicy.computeEffectiveBudget(
            settings = AppSettings(
                processingQuality = ProcessingQuality.HIGH,
                batterySavingMode = true
            ),
            thermalLevel = ThermalLevel.NONE,
            batteryTempCelsius = 30.0f
        )
        assertEquals(15, budget.targetFpsCap)
        assertEquals(ProcessingQuality.LOW, budget.processingQuality)
        assertTrue(budget.isDownscaled)
    }
}
