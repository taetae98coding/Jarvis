package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/** 무엇을 어떤 API 로 읽는지는 각 플랫폼 스펙의 battery 절에 있다. cold 로 내놓는다. */
internal fun interface BatterySource {
    fun observe(): Flow<BatteryStatus>
}

internal expect fun createBatterySource(context: PlatformContext): BatterySource

/** Android 의 `EXTRA_LEVEL / EXTRA_SCALE`. 음수·0 스케일은 읽기 실패다. */
internal fun levelPercent(level: Int, scale: Int): Int? =
    if (level < 0 || scale <= 0) null else (level * 100.0 / scale).roundToInt().coerceIn(0, 100)

/** iOS·Web 의 0–1 비율. iOS 는 모를 때 -1 을 준다. */
internal fun levelPercent(fraction: Double): Int? =
    if (fraction.isNaN() || fraction < 0) null else (fraction * 100).roundToInt().coerceIn(0, 100)
