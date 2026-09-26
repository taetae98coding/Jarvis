package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeSystemState
import io.github.taetae98coding.jarvis.domain.worldclock.TimeZoneRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** 어떤 API 로 읽는지는 각 플랫폼 스펙의 world-clock 절에 있다. */
internal class PlatformTimeZoneRepository(
    private val changes: Flow<Unit>?,
) : TimeZoneRepository {
    override fun offsetSecondsAt(zoneId: String, instant: Instant): Int? = zoneOffsetSeconds(zoneId, instant.epochSeconds)

    override fun observeLocalZoneId(): Flow<String> =
        observeSystemState(signals = changes, interval = LocalZonePollInterval, read = ::systemZoneId)

    override fun readLocalZoneId(): String = systemZoneId()
}

/** [epochSeconds] 순간에 [zoneId] 가 UTC 보다 몇 초 앞서는지. 모르는 시간대면 null. */
internal expect fun zoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int?

/** 기기 시간대의 IANA 이름. 부를 때마다 새로 읽는다(캐시된 기본값을 돌려주지 않는다). */
internal expect fun systemZoneId(): String

/** 기기 시간대가 바뀌었다는 신호. cold 다. 알려 주지 않는 플랫폼은 null 이고 [LocalZonePollInterval] 로 읽는다. */
internal expect fun systemZoneChanges(context: PlatformContext): Flow<Unit>?

/*
 * JVM(macOS)·Web 은 시간대 변경을 알려 주는 API 가 없다(docs/platform/jvm.html#world-clock, web.html#world-clock).
 * 시간대는 여행하거나 설정을 바꿀 때만 바뀌어, 시계가 1초마다 도는 동안에도 몇 초 늦게 따라가면 충분하다.
 */
internal val LocalZonePollInterval = 5.seconds
