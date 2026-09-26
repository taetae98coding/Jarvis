package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import io.github.taetae98coding.jarvis.domain.focus.FocusSession
import io.github.taetae98coding.jarvis.domain.focus.FocusTimer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FocusSessionRepositoryTest {
    @Test
    fun roundTripsEveryTimerKind() {
        listOf(
            FocusSession(),
            FocusSession(
                phase = FocusPhase.LONG_BREAK,
                timer = FocusTimer.Running(Instant.fromEpochMilliseconds(1_790_000_000_123)),
                focusesInCycle = 4,
                todayCount = 7,
                todayEpochDay = 20_722,
            ),
            FocusSession(phase = FocusPhase.SHORT_BREAK, timer = FocusTimer.Paused(3.minutes + 12.seconds), focusesInCycle = 1),
        ).forEach { session ->
            assertEquals(session, decodeFocusSession(encodeFocusSession(session)))
        }
    }

    @Test
    fun unknownValueIsInitialSession() {
        listOf("", "garbage", "2|focus|i|0|0|0|0", "1|nap|i|0|0|0|0", "1|focus|x|0|0|0|0", "1|focus|r|soon|0|0|0").forEach {
            assertEquals(FocusSession(), decodeFocusSession(it))
        }
    }

    @Test
    fun survivesRestart() = runTest {
        val store = InMemorySettingsStore()
        val session = FocusSession(timer = FocusTimer.Paused(10.minutes), todayCount = 2, todayEpochDay = 3)
        DefaultFocusSessionRepository(store).saveFocusSession(session)

        val reopened = DefaultFocusSessionRepository(store)
        assertEquals(session, reopened.readFocusSession())
        assertEquals(session, reopened.observeFocusSession().first())
    }

    @Test
    fun observeFollowsSaves() = runTest {
        val store = InMemorySettingsStore()
        val repository = DefaultFocusSessionRepository(store)
        val running = FocusSession(timer = FocusTimer.Running(Instant.fromEpochSeconds(100)))

        val seen = mutableListOf<FocusSession>()
        val job = launch { repository.observeFocusSession().take(2).toList(seen) }
        runCurrent()
        repository.saveFocusSession(running)
        job.join()

        assertEquals(listOf(FocusSession(), running), seen)
        assertEquals(0, store.listeners)
    }
}
