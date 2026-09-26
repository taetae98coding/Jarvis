package io.github.taetae98coding.jarvis.ui.battery

import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryFormatTest {
    @Test
    fun levelIsClampedPercent() {
        assertEquals("87%", formatLevel(87))
        assertEquals("100%", formatLevel(120))
        assertEquals("0%", formatLevel(-3))
    }

    @Test
    fun celsiusKeepsOneDecimalAndDropsZero() {
        assertEquals("31.5°C", formatCelsius(31.5))
        assertEquals("30°C", formatCelsius(30.0))
        assertEquals("-2.5°C", formatCelsius(-2.5))
    }
}
