package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.focus.FocusClock
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal object SystemFocusClock : FocusClock {
    override fun now(): Instant = Clock.System.now()

    override fun observeNow(): Flow<Instant> = observeByPolling(FocusTickInterval, ::now)

    override fun localEpochDay(instant: Instant): Long =
        (instant.epochSeconds + localUtcOffsetSeconds(instant)).floorDiv(SecondsPerDay)
}

/*
 * 시계는 흐름을 알려주는 콜백이 없어 폴링이다(docs/common/state-observation.html R7). 1초 대신 1/4초인 것은
 * 폴링이 초 경계와 어긋나 시작해도 mm:ss 가 실제 경계에서 1/4초 안에 바뀌게 하려는 것이다. 남은 시간은 초로
 * 올림하므로 같은 초 안의 방출은 ViewModel 의 StateFlow 에서 걸러진다.
 */
internal val FocusTickInterval = 250.milliseconds

private const val SecondsPerDay = 86_400L

/** [instant] 시각에 기기 시간대가 UTC 보다 몇 초 앞서는지. 서머타임이 있어 시각마다 다를 수 있다. */
internal expect fun localUtcOffsetSeconds(instant: Instant): Int
