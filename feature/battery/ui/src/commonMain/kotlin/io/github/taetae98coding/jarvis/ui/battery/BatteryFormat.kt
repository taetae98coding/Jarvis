package io.github.taetae98coding.jarvis.ui.battery

import io.github.taetae98coding.jarvis.domain.battery.BatteryHealth
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatLevel(percent: Int): String = "${percent.coerceIn(0, 100)}%"

/** 소수 한 자리까지 적고 `.0` 은 뗀다. */
internal fun formatCelsius(celsius: Double): String {
    val tenths = (celsius * 10).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    val whole = abs(tenths) / 10
    val fraction = abs(tenths) % 10

    return if (fraction == 0) "$sign$whole°C" else "$sign$whole.$fraction°C"
}

internal val ChargingState.label: String
    get() = when (this) {
        ChargingState.CHARGING -> "충전 중"
        ChargingState.DISCHARGING -> "방전 중"
        ChargingState.FULL -> "완충"
        ChargingState.NOT_CHARGING -> "전원 연결됨 (충전 안 함)"
    }

internal val PowerSource.label: String
    get() = when (this) {
        PowerSource.AC -> "전원 어댑터"
        PowerSource.USB -> "USB"
        PowerSource.WIRELESS -> "무선 충전"
        PowerSource.DOCK -> "도크"
        PowerSource.BATTERY -> "배터리"
    }

internal val BatteryHealth.label: String
    get() = when (this) {
        BatteryHealth.GOOD -> "좋음"
        BatteryHealth.OVERHEAT -> "과열"
        BatteryHealth.DEAD -> "수명 다함"
        BatteryHealth.OVER_VOLTAGE -> "과전압"
        BatteryHealth.COLD -> "저온"
        BatteryHealth.FAILURE -> "고장"
    }

internal fun formatOnOff(on: Boolean): String = if (on) "켜짐" else "꺼짐"
