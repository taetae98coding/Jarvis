package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.worldclock.ClockRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal object SystemClockRepository : ClockRepository {
    override fun observeNow(): Flow<Instant> = observeByPolling(WorldClockTickInterval, ::readNow)

    override fun readNow(): Instant = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
}

/*
 * 시간이 흐르는 것을 알려 주는 콜백은 없어 폴링이다(docs/common/state-observation.html R7). 1초가 아니라 1/4초인 것은
 * 폴링이 초 경계와 어긋나 시작해도 화면의 초가 실제 경계에서 1/4초 안에 바뀌게 하려는 것이다. 읽은 값을 초로
 * 자르므로 같은 초 안의 방출은 observeByPolling 의 distinctUntilChanged 가 거른다 — 아래로는 1초에 한 번만 흐른다.
 */
internal val WorldClockTickInterval = 250.milliseconds
