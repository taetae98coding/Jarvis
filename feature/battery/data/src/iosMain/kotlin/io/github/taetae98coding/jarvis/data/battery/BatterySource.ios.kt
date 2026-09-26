package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoPowerStateDidChangeNotification
import platform.Foundation.isLowPowerModeEnabled
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification

internal actual fun createBatterySource(context: PlatformContext): BatterySource = UIDeviceBatterySource

private object UIDeviceBatterySource : BatterySource {
    // UIDevice 는 메인 스레드에서만 만진다.
    override fun observe(): Flow<BatteryStatus> = flow {
        val device = UIDevice.currentDevice
        // 첫 읽기보다 먼저 켜야 한다. 꺼져 있으면 batteryLevel 이 -1, batteryState 가 Unknown 이다.
        device.batteryMonitoringEnabled = true

        try {
            emitAll(observeOnSignals(changes(), ::read))
        } finally {
            device.batteryMonitoringEnabled = false
        }
    }.flowOn(Dispatchers.Main)

    private fun changes(): Flow<Unit> = callbackFlow {
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(
            UIDeviceBatteryLevelDidChangeNotification,
            UIDeviceBatteryStateDidChangeNotification,
            NSProcessInfoPowerStateDidChangeNotification,
        ).map { name ->
            center.addObserverForName(name = name, `object` = null, queue = null) { trySend(Unit) }
        }

        awaitClose { observers.forEach(center::removeObserver) }
    }

    private fun read(): BatteryStatus {
        val device = UIDevice.currentDevice
        val charging = when (device.batteryState) {
            UIDeviceBatteryState.UIDeviceBatteryStateCharging -> ChargingState.CHARGING
            UIDeviceBatteryState.UIDeviceBatteryStateFull -> ChargingState.FULL
            UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> ChargingState.DISCHARGING
            // 시뮬레이터는 늘 Unknown 이다.
            else -> return BatteryStatus.Unavailable
        }
        val level = levelPercent(device.batteryLevel.toDouble()) ?: return BatteryStatus.Unavailable

        return BatteryStatus.Available(
            Battery(
                levelPercent = level,
                charging = charging,
                // 꽂혀 있을 때 어댑터 종류는 공개 API 가 알려 주지 않는다.
                powerSource = if (charging == ChargingState.DISCHARGING) PowerSource.BATTERY else null,
                lowPowerMode = NSProcessInfo.processInfo.isLowPowerModeEnabled(),
            ),
        )
    }
}
