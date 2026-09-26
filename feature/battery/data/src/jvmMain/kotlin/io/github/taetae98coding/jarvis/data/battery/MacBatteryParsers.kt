package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource

/**
 * `pmset -g batt` 출력. 배터리가 있으면 둘째 줄이 이렇게 생겼다.
 *
 * ```
 * Now drawing from 'Battery Power'
 *  -InternalBattery-0 (id=4653155)	87%; discharging; 5:12 remaining present: true
 * ```
 *
 * 배터리가 없는 Mac 은 첫 줄만 있다. [lowPowerMode] 는 `pmset -g` 에서 따로 읽은 값이다.
 */
internal fun parsePmsetBattery(output: String, lowPowerMode: Boolean?): BatteryStatus {
    val source = DrawingFrom.find(output)?.groupValues?.get(1) ?: return BatteryStatus.Unavailable
    val line = InternalBattery.find(output) ?: return BatteryStatus.NoBattery
    val powerSource = when (source) {
        "AC Power" -> PowerSource.AC
        "Battery Power" -> PowerSource.BATTERY
        else -> null
    }
    val level = line.groupValues[1].toInt().coerceIn(0, 100)
    val charging = when (line.groupValues[2].trim()) {
        "charging", "finishing charge" -> ChargingState.CHARGING
        "discharging" -> ChargingState.DISCHARGING
        "charged" -> ChargingState.FULL
        // 최적화 충전·충전 한도로 멈췄을 때다.
        "AC attached" -> ChargingState.NOT_CHARGING
        else -> if (powerSource == PowerSource.BATTERY) ChargingState.DISCHARGING else ChargingState.NOT_CHARGING
    }

    return BatteryStatus.Available(
        Battery(levelPercent = level, charging = charging, powerSource = powerSource, lowPowerMode = lowPowerMode),
    )
}

/** `pmset -g` 의 `lowpowermode 0|1` 줄. macOS 12 이전이나 Intel 데스크톱은 줄이 없어 null 이다. */
internal fun parsePmsetLowPowerMode(output: String): Boolean? =
    LowPowerMode.find(output)?.groupValues?.get(1)?.let { it == "1" }

private val DrawingFrom = Regex("""Now drawing from '([^']+)'""")

private val InternalBattery = Regex("""-InternalBattery-\d+[^\t]*\t\s*(\d+)%;\s*([^;]+);""")

private val LowPowerMode = Regex("""(?m)^\s*lowpowermode\s+(\d)""")
