package io.github.taetae98coding.jarvis.domain.battery

import kotlinx.coroutines.flow.Flow

/** `observeBattery` 는 cold 라서 수집하는 동안에만 플랫폼 알림을 받는다. */
interface BatteryRepository {
    fun observeBattery(): Flow<BatteryStatus>
}
