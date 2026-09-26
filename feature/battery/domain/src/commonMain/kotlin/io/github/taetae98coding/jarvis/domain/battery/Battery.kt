package io.github.taetae98coding.jarvis.domain.battery

/** 이 앱이 돌고 있는 기기의 배터리. 플랫폼이 주지 않는 항목은 null 이다. */
data class Battery(
    val levelPercent: Int,
    val charging: ChargingState,
    val powerSource: PowerSource? = null,
    val temperatureCelsius: Double? = null,
    val health: BatteryHealth? = null,
    val lowPowerMode: Boolean? = null,
) {
    /** 전원 없이 [LowBatteryPercent] 이하로 내려간 상태. 카드가 잔량을 경고 색으로 그린다. */
    val isLow: Boolean
        get() = levelPercent <= LowBatteryPercent &&
            charging != ChargingState.CHARGING &&
            charging != ChargingState.FULL

    companion object {
        // Android·iOS 가 저전력 모드를 권하는 기준과 같다.
        const val LowBatteryPercent = 20
    }
}

enum class ChargingState {
    CHARGING,
    DISCHARGING,
    FULL,

    /** 전원은 연결됐지만 충전하지 않는다. macOS 최적화 충전, Android 충전 보호가 멈춘 상태. */
    NOT_CHARGING,
}

enum class PowerSource {
    AC,
    USB,
    WIRELESS,
    DOCK,
    BATTERY,
}

enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    COLD,
    FAILURE,
}

sealed interface BatteryStatus {
    /** 첫 값이 오기 전. */
    data object Loading : BatteryStatus

    /** 배터리가 없는 기기(데스크톱 Mac, 배터리 없는 PC 의 브라우저). */
    data object NoBattery : BatteryStatus

    /** 플랫폼이 알려 주지 않거나 읽기에 실패했다. */
    data object Unavailable : BatteryStatus

    data class Available(val battery: Battery) : BatteryStatus
}
