package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource
import kotlin.test.Test
import kotlin.test.assertEquals

class MacBatteryParsersTest {
    @Test
    fun desktopMacHasNoBattery() {
        // 2026-09-26 Mac Studio(macOS 26) 에서 받은 출력.
        assertEquals(BatteryStatus.NoBattery, parsePmsetBattery("Now drawing from 'AC Power'\n", lowPowerMode = false))
    }

    @Test
    fun discharging() {
        val output = "Now drawing from 'Battery Power'\n" +
            " -InternalBattery-0 (id=4653155)\t87%; discharging; 5:12 remaining present: true\n"

        assertEquals(
            BatteryStatus.Available(
                Battery(levelPercent = 87, charging = ChargingState.DISCHARGING, powerSource = PowerSource.BATTERY, lowPowerMode = true),
            ),
            parsePmsetBattery(output, lowPowerMode = true),
        )
    }

    @Test
    fun chargingStates() {
        fun charging(state: String): ChargingState {
            val output = "Now drawing from 'AC Power'\n" +
                " -InternalBattery-0 (id=4653155)\t64%; $state; 1:05 remaining present: true\n"
            return ((parsePmsetBattery(output, lowPowerMode = null) as BatteryStatus.Available).battery).charging
        }

        assertEquals(ChargingState.CHARGING, charging("charging"))
        assertEquals(ChargingState.CHARGING, charging("finishing charge"))
        assertEquals(ChargingState.FULL, charging("charged"))
        assertEquals(ChargingState.NOT_CHARGING, charging("AC attached"))
    }

    @Test
    fun acPowerSource() {
        val output = "Now drawing from 'AC Power'\n" +
            " -InternalBattery-0 (id=4653155)\t100%; charged; 0:00 remaining present: true\n"

        assertEquals(
            PowerSource.AC,
            ((parsePmsetBattery(output, lowPowerMode = null) as BatteryStatus.Available).battery).powerSource,
        )
    }

    @Test
    fun unreadableOutputIsUnavailable() {
        assertEquals(BatteryStatus.Unavailable, parsePmsetBattery("", lowPowerMode = null))
    }

    @Test
    fun lowPowerMode() {
        // 2026-09-26 Mac Studio(macOS 26) 의 pmset -g 일부.
        val output = "System-wide power settings:\nCurrently in use:\n standby              0\n" +
            " displaysleep         10\n tcpkeepalive         1\n lowpowermode         0\n womp                 1\n"

        assertEquals(false, parsePmsetLowPowerMode(output))
        assertEquals(true, parsePmsetLowPowerMode(output.replace("lowpowermode         0", "lowpowermode         1")))
        assertEquals(null, parsePmsetLowPowerMode("Currently in use:\n standby 0\n"))
    }
}
