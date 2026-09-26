package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import io.github.taetae98coding.jarvis.domain.focus.FocusSession
import io.github.taetae98coding.jarvis.domain.focus.FocusTimer
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerPhaseTestTag
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerPrimaryTestTag
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerRemainingTestTag
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerResetTestTag
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerSkipTestTag
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerTodayTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class JarvisAppFocusTimerTest {
    @Test
    fun startsAtTwentyFiveMinutesOfFocus() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(FocusTimerPhaseTestTag).assertTextEquals("집중")
        onNodeWithTag(FocusTimerRemainingTestTag).assertTextEquals("25:00")
        onNodeWithTag(FocusTimerPrimaryTestTag).assertContentDescriptionEquals("시작")
        onNodeWithTag(FocusTimerTodayTestTag).assertTextEquals("오늘 마친 집중", "0회")
    }

    @Test
    fun startCountsDownAndSchedulesAlarm() = runComposeUiTest {
        val clock = FakeFocusClock()
        val alarm = FakeFocusAlarmRepository()
        setContent { TestJarvisApp(focusClock = clock, focusAlarm = alarm) }

        onNodeWithTag(FocusTimerPrimaryTestTag).performClick()
        waitForIdle()
        assertEquals(listOf(clock.now() + 25.minutes to FocusPhase.FOCUS), alarm.scheduled)

        clock.time.value += 61.seconds
        waitForIdle()
        onNodeWithTag(FocusTimerRemainingTestTag).assertTextEquals("23:59")
        onNodeWithTag(FocusTimerPrimaryTestTag).assertContentDescriptionEquals("일시정지")
    }

    @Test
    fun pauseAndResume() = runComposeUiTest {
        val clock = FakeFocusClock()
        val alarm = FakeFocusAlarmRepository()
        setContent { TestJarvisApp(focusClock = clock, focusAlarm = alarm) }

        onNodeWithTag(FocusTimerPrimaryTestTag).performClick()
        waitForIdle()
        clock.time.value += 5.minutes
        onNodeWithTag(FocusTimerPrimaryTestTag).performClick()
        waitForIdle()
        clock.time.value += 1.minutes
        waitForIdle()

        onNodeWithTag(FocusTimerRemainingTestTag).assertTextEquals("20:00")
        onNodeWithTag(FocusTimerPrimaryTestTag).assertContentDescriptionEquals("재개").performClick()
        waitForIdle()
        assertEquals(clock.now() + 20.minutes to FocusPhase.FOCUS, alarm.scheduled.last())
        assertEquals(1, alarm.cancels)
    }

    @Test
    fun focusThatEndedWhileClosedShowsBreak() = runComposeUiTest {
        val clock = FakeFocusClock()
        val session = FakeFocusSessionRepository(
            FocusSession(timer = FocusTimer.Running(clock.now() - 1.minutes), todayEpochDay = clock.localEpochDay(clock.now())),
        )
        setContent { TestJarvisApp(focusSession = session, focusClock = clock) }

        onNodeWithTag(FocusTimerPhaseTestTag).assertTextEquals("짧은 휴식")
        onNodeWithTag(FocusTimerRemainingTestTag).assertTextEquals("05:00")
        onNodeWithTag(FocusTimerTodayTestTag).assertTextEquals("오늘 마친 집중", "1회")
    }

    @Test
    fun skipAndReset() = runComposeUiTest {
        setContent { TestJarvisApp() }

        onNodeWithTag(FocusTimerSkipTestTag).performClick()
        waitForIdle()
        onNodeWithTag(FocusTimerPhaseTestTag).assertTextEquals("짧은 휴식")

        onNodeWithTag(FocusTimerResetTestTag).performClick()
        waitForIdle()
        onNodeWithTag(FocusTimerPhaseTestTag).assertTextEquals("집중")
        onNodeWithTag(FocusTimerRemainingTestTag).assertTextEquals("25:00")
    }
}
