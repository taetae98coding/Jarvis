package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryRepositoryTest {
    @Test
    fun dropsRepeatedValues() = runTest {
        val battery = BatteryStatus.Available(Battery(levelPercent = 80, charging = ChargingState.DISCHARGING))
        val lower = BatteryStatus.Available(Battery(levelPercent = 79, charging = ChargingState.DISCHARGING))
        val repository = DefaultBatteryRepository { flowOf(battery, battery, lower, lower) }

        assertEquals(listOf(battery, lower), repository.observeBattery().toList())
    }

    @Test
    fun opensTheSourceOnlyWhileCollected() = runTest {
        var opened = 0
        val repository = DefaultBatteryRepository {
            flow {
                opened++
                emit(BatteryStatus.NoBattery)
            }
        }
        val battery = repository.observeBattery()

        assertEquals(0, opened)
        battery.toList()
        assertEquals(1, opened)
    }

    @Test
    fun levelFromLevelAndScale() {
        assertEquals(87, levelPercent(level = 87, scale = 100))
        assertEquals(50, levelPercent(level = 128, scale = 256))
        assertEquals(null, levelPercent(level = -1, scale = 100))
        assertEquals(null, levelPercent(level = 50, scale = 0))
    }

    @Test
    fun levelFromFraction() {
        assertEquals(87, levelPercent(0.87))
        assertEquals(100, levelPercent(1.0))
        assertEquals(null, levelPercent(-1.0))
        assertEquals(null, levelPercent(Double.NaN))
    }
}
