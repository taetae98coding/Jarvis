package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.domain.battery.BatteryRepository
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class DefaultBatteryRepository(
    private val source: BatterySource,
) : BatteryRepository {
    // Android 의 ACTION_BATTERY_CHANGED 는 전압만 바뀌어도 오므로 같은 값은 거른다.
    override fun observeBattery(): Flow<BatteryStatus> = source.observe().distinctUntilChanged()
}
