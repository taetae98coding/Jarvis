package io.github.taetae98coding.jarvis.domain.focus

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FocusTimerTest {
    private val clock = FakeFocusClock()
    private val repository = FakeFocusSessionRepository()
    private val alarm = FakeFocusAlarmRepository()

    private val start = StartFocusTimerUseCase(repository, alarm, clock)
    private val pause = PauseFocusTimerUseCase(repository, alarm, clock)
    private val resume = ResumeFocusTimerUseCase(repository, alarm, clock)
    private val reset = ResetFocusTimerUseCase(repository, alarm, clock)
    private val skip = SkipFocusPhaseUseCase(repository, alarm, clock)

    private fun state(): FocusTimerState = FocusTimerState.of(repository.session.value, clock.now(), clock)

    @Test
    fun idleShowsFullFocusDuration() {
        val state = state()

        assertEquals(FocusPhase.FOCUS, state.phase)
        assertEquals(FocusStatus.IDLE, state.status)
        assertEquals(25.minutes, state.remaining)
        assertEquals(0f, state.progress)
    }

    @Test
    fun startSchedulesAlarmAtEnd() = runTest {
        start()

        assertEquals(clock.now() + 25.minutes to FocusPhase.FOCUS, alarm.scheduled)
        clock.advance(10.minutes)
        assertEquals(FocusStatus.RUNNING, state().status)
        assertEquals(15.minutes, state().remaining)
    }

    @Test
    fun remainingRoundsUpToWholeSeconds() = runTest {
        start()
        clock.advance(400.milliseconds)

        assertEquals(25.minutes, state().remaining)
    }

    @Test
    fun pauseFreezesAndResumeContinues() = runTest {
        start()
        clock.advance(5.minutes)
        pause()

        assertNull(alarm.scheduled)
        clock.advance(1.hours)
        assertEquals(FocusStatus.PAUSED, state().status)
        assertEquals(20.minutes, state().remaining)

        resume()
        assertEquals(clock.now() + 20.minutes to FocusPhase.FOCUS, alarm.scheduled)
        clock.advance(20.minutes)
        assertEquals(FocusPhase.SHORT_BREAK, state().phase)
    }

    @Test
    fun focusEndCountsAndMovesToShortBreakWithoutStarting() = runTest {
        start()
        clock.advance(25.minutes)

        val state = state()
        assertEquals(FocusPhase.SHORT_BREAK, state.phase)
        assertEquals(FocusStatus.IDLE, state.status)
        assertEquals(5.minutes, state.remaining)
        assertEquals(1, state.todayCount)
        assertEquals(1, state.focusesInCycle)
    }

    @Test
    fun everyFourthFocusIsFollowedByLongBreak() = runTest {
        repeat(3) {
            start()
            clock.advance(25.minutes)
            assertEquals(FocusPhase.SHORT_BREAK, state().phase)
            start()
            clock.advance(5.minutes)
            assertEquals(FocusPhase.FOCUS, state().phase)
        }

        start()
        clock.advance(25.minutes)
        assertEquals(FocusPhase.LONG_BREAK, state().phase)
        assertEquals(15.minutes, state().remaining)
        assertEquals(4, state().todayCount)

        start()
        clock.advance(15.minutes)
        assertEquals(FocusPhase.FOCUS, state().phase)
        assertEquals(0, state().focusesInCycle)
        assertEquals(4, state().todayCount)
    }

    @Test
    fun endedWhileClosedAdvancesOnlyOnce() = runTest {
        start()
        // 앱이 꺼진 채 몇 시간이 지났다. 휴식은 저절로 시작하지 않았으므로 휴식 대기에 머문다.
        clock.advance(3.hours)

        assertEquals(FocusPhase.SHORT_BREAK, state().phase)
        assertEquals(FocusStatus.IDLE, state().status)
        assertEquals(1, state().todayCount)
    }

    @Test
    fun commandSettlesBeforeActing() = runTest {
        start()
        clock.advance(30.minutes)
        // 이미 끝난 집중을 일시정지하지 않는다. 끝난 것을 넘긴 뒤 휴식 대기라 아무것도 하지 않는다.
        pause()

        assertEquals(FocusPhase.SHORT_BREAK, state().phase)
        assertEquals(FocusStatus.IDLE, state().status)
        assertEquals(1, state().todayCount)

        // 끝난 단계를 넘긴 뒤 시작하므로 휴식이 시작된다.
        start()
        assertEquals(clock.now() + 5.minutes to FocusPhase.SHORT_BREAK, alarm.scheduled)
        assertEquals(1, repository.session.value.todayCount)
    }

    @Test
    fun skipDoesNotCount() = runTest {
        start()
        skip()

        assertEquals(FocusPhase.SHORT_BREAK, state().phase)
        assertEquals(0, state().todayCount)
        assertNull(alarm.scheduled)

        skip()
        assertEquals(FocusPhase.FOCUS, state().phase)
    }

    @Test
    fun resetKeepsTodayCount() = runTest {
        start()
        clock.advance(25.minutes)
        start()
        reset()

        assertEquals(FocusPhase.FOCUS, state().phase)
        assertEquals(FocusStatus.IDLE, state().status)
        assertEquals(0, state().focusesInCycle)
        assertEquals(1, state().todayCount)
        assertNull(alarm.scheduled)
    }

    @Test
    fun todayCountResetsOnNextDay() = runTest {
        start()
        clock.advance(25.minutes)
        assertEquals(1, state().todayCount)

        clock.advance(24.hours)
        assertEquals(0, state().todayCount)

        start()
        clock.advance(5.minutes)
        start()
        clock.advance(25.minutes)
        assertEquals(1, state().todayCount)
    }

    @Test
    fun focusThatEndedYesterdayCountsForYesterday() = runTest {
        // 자정 30분 전에 시작해 자정 전에 끝났고, 앱은 다음 날 켰다.
        clock.time.value = Instant.fromEpochSeconds(FakeFocusClock.Day * 101 - 30 * 60)
        start()
        clock.advance(10.hours)

        assertEquals(0, state().todayCount)
        val settled = repository.session.value.settle(clock.now(), clock)
        assertEquals(1, settled.todayCount)
        assertEquals(100, settled.todayEpochDay)
    }

    @Test
    fun startIsIgnoredWhileRunning() = runTest {
        start()
        clock.advance(1.seconds)
        start()

        assertEquals(24.minutes + 59.seconds, state().remaining)
    }
}
