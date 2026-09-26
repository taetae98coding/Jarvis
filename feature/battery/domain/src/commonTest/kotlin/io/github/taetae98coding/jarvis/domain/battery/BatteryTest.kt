package io.github.taetae98coding.jarvis.domain.battery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatteryTest {
    @Test
    fun lowWhenAtOrBelowThresholdWithoutPower() {
        assertTrue(Battery(levelPercent = 20, charging = ChargingState.DISCHARGING).isLow)
        assertTrue(Battery(levelPercent = 5, charging = ChargingState.NOT_CHARGING).isLow)
    }

    @Test
    fun notLowAboveThreshold() {
        assertFalse(Battery(levelPercent = 21, charging = ChargingState.DISCHARGING).isLow)
    }

    @Test
    fun notLowWhileChargingOrFull() {
        assertFalse(Battery(levelPercent = 10, charging = ChargingState.CHARGING).isLow)
        assertFalse(Battery(levelPercent = 10, charging = ChargingState.FULL).isLow)
    }
}
