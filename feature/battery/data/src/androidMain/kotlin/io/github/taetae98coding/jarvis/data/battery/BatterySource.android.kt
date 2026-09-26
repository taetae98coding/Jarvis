package io.github.taetae98coding.jarvis.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryHealth
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal actual fun createBatterySource(context: PlatformContext): BatterySource =
    AndroidBatterySource(context.context.applicationContext)

private class AndroidBatterySource(
    private val context: Context,
) : BatterySource {
    private val powerManager: PowerManager? = context.getSystemService(PowerManager::class.java)

    override fun observe(): Flow<BatteryStatus> = observeOnSignals(changes(), ::read)

    private fun changes(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }

        // 보호된 시스템 브로드캐스트는 NOT_EXPORTED 로도 온다. 플래그를 빼면 Android 14 부터 예외다.
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun read(): BatteryStatus {
        // ACTION_BATTERY_CHANGED 는 sticky 라 리시버 없이 등록하면 마지막 값을 곧바로 돌려준다.
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryStatus.Unavailable

        if (!intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)) return BatteryStatus.NoBattery

        val level = levelPercent(
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        ) ?: return BatteryStatus.Unavailable
        val source = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1).toPowerSource()
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)

        return BatteryStatus.Available(
            Battery(
                levelPercent = level,
                charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1).toChargingState(source),
                powerSource = source,
                // 0.1°C 단위다.
                temperatureCelsius = if (temperature == Int.MIN_VALUE) null else temperature / 10.0,
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1).toHealth(),
                lowPowerMode = powerManager?.isPowerSaveMode,
            ),
        )
    }
}

private fun Int.toPowerSource(): PowerSource? = when (this) {
    0 -> PowerSource.BATTERY
    BatteryManager.BATTERY_PLUGGED_AC -> PowerSource.AC
    BatteryManager.BATTERY_PLUGGED_USB -> PowerSource.USB
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> PowerSource.WIRELESS
    BatteryManager.BATTERY_PLUGGED_DOCK -> PowerSource.DOCK
    else -> null
}

private fun Int.toChargingState(source: PowerSource?): ChargingState = when (this) {
    BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.CHARGING
    BatteryManager.BATTERY_STATUS_FULL -> ChargingState.FULL
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.NOT_CHARGING
    BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingState.DISCHARGING
    // UNKNOWN 은 전원이 꽂혀 있는지로 가른다.
    else -> if (source == null || source == PowerSource.BATTERY) ChargingState.DISCHARGING else ChargingState.NOT_CHARGING
}

private fun Int.toHealth(): BatteryHealth? = when (this) {
    BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
    BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
    BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
    else -> null
}
