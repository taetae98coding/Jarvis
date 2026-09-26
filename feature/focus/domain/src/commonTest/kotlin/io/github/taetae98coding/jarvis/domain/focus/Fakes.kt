package io.github.taetae98coding.jarvis.domain.focus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.Instant

/** 날의 경계는 UTC 자정이다. */
internal class FakeFocusClock(start: Instant = Instant.fromEpochSeconds(Day * 100)) : FocusClock {
    val time = MutableStateFlow(start)

    override fun now(): Instant = time.value

    override fun observeNow(): Flow<Instant> = time

    override fun localEpochDay(instant: Instant): Long = instant.epochSeconds.floorDiv(Day)

    fun advance(by: Duration) {
        time.value += by
    }

    companion object {
        const val Day = 86_400L
    }
}

internal class FakeFocusSessionRepository(initial: FocusSession = FocusSession()) : FocusSessionRepository {
    val session = MutableStateFlow(initial)

    override fun observeFocusSession(): Flow<FocusSession> = session

    override fun readFocusSession(): FocusSession = session.value

    override fun saveFocusSession(session: FocusSession) {
        this.session.value = session
    }
}

internal class FakeFocusAlarmRepository : FocusAlarmRepository {
    var scheduled: Pair<Instant, FocusPhase>? = null
        private set

    var cancels = 0
        private set

    override fun schedule(at: Instant, phase: FocusPhase) {
        scheduled = at to phase
    }

    override fun cancel() {
        scheduled = null
        cancels++
    }
}
