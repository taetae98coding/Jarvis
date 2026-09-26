package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.battery.Battery
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ChargingState
import io.github.taetae98coding.jarvis.domain.battery.PowerSource
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import kotlin.js.Promise

internal actual fun createBatterySource(context: PlatformContext): BatterySource = BrowserBatterySource

// Battery Status API 는 Chromium 에만 있다. Safari·Firefox 는 측정 불가다(docs/platform/web.html#battery).
private object BrowserBatterySource : BatterySource {
    override fun observe(): Flow<BatteryStatus> = flow {
        val manager = runCatching { batteryManager()?.await<JsAny>() }.getOrNull()
        if (manager == null) {
            emit(BatteryStatus.Unavailable)
            return@flow
        }

        emitAll(observeOnSignals(changes(manager.unsafeCast<EventTarget>())) { read(manager) })
    }

    private fun changes(target: EventTarget): Flow<Unit> = callbackFlow {
        val listener: (Event) -> Unit = { trySend(Unit) }
        BatteryEvents.forEach { target.addEventListener(it, listener) }
        awaitClose { BatteryEvents.forEach { target.removeEventListener(it, listener) } }
    }

    private fun read(manager: JsAny): BatteryStatus {
        val level = levelPercent(batteryLevel(manager)) ?: return BatteryStatus.Unavailable
        val charging = batteryCharging(manager)

        return BatteryStatus.Available(
            Battery(
                levelPercent = level,
                // 브라우저는 "충전 중" 과 "전원 연결됨" 을 나누지 않는다. 꽂혀 있고 가득 찼으면 완충이다.
                charging = when {
                    !charging -> ChargingState.DISCHARGING
                    level >= 100 -> ChargingState.FULL
                    else -> ChargingState.CHARGING
                },
                powerSource = if (charging) null else PowerSource.BATTERY,
            ),
        )
    }
}

private val BatteryEvents = listOf("levelchange", "chargingchange")

private fun batteryManager(): Promise<JsAny>? =
    js("((navigator.getBattery) ? navigator.getBattery() : null)")

private fun batteryLevel(manager: JsAny): Double = js("(typeof manager.level === 'number' ? manager.level : -1)")

private fun batteryCharging(manager: JsAny): Boolean = js("(manager.charging === true)")
