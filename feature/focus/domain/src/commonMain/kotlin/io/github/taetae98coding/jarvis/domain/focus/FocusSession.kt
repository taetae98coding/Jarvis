package io.github.taetae98coding.jarvis.domain.focus

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * 저장되는 타이머 상태. 남은 시간 대신 끝나는 시각을 적어 두므로 앱이 꺼져 있던 시간도 벽시계로 다시 센다.
 *
 * [focusesInCycle] 은 마지막 긴 휴식 뒤로 끝까지 마친 집중 수, [todayCount] 는 [todayEpochDay] 날에 마친 집중 수다.
 */
data class FocusSession(
    val phase: FocusPhase = FocusPhase.FOCUS,
    val timer: FocusTimer = FocusTimer.Idle,
    val focusesInCycle: Int = 0,
    val todayCount: Int = 0,
    val todayEpochDay: Long = 0,
) {
    /**
     * 끝나는 시각이 지났으면 그 단계를 마친 것으로 넘긴다. 다음 단계는 저절로 시작하지 않으므로 한 번만 넘어간다.
     * 저장값은 그대로 두고 읽을 때마다 푼다. 그래서 관찰이 쓰기를 하지 않는다.
     */
    fun settle(now: Instant, clock: FocusClock): FocusSession {
        val timer = timer
        if (timer !is FocusTimer.Running || timer.endsAt > now) return this

        return if (phase == FocusPhase.FOCUS) {
            // 앱이 꺼진 동안 끝났으면 끝난 날에 센다. 자정을 넘겨 켜도 어제 기록이 오늘로 오지 않는다.
            val endDay = clock.localEpochDay(timer.endsAt)
            val completed = focusesInCycle + 1
            copy(
                phase = nextPhaseAfterFocus(completed),
                timer = FocusTimer.Idle,
                focusesInCycle = completed,
                todayCount = if (endDay == todayEpochDay) todayCount + 1 else 1,
                todayEpochDay = endDay,
            )
        } else {
            afterBreak()
        }
    }

    /** 건너뛰기. 끝까지 마친 것이 아니라 집중 횟수는 늘지 않는다. */
    fun skipped(): FocusSession =
        if (phase == FocusPhase.FOCUS) {
            copy(phase = nextPhaseAfterFocus(focusesInCycle), timer = FocusTimer.Idle)
        } else {
            afterBreak()
        }

    fun todayCount(today: Long): Int = if (todayEpochDay == today) todayCount else 0

    private fun afterBreak(): FocusSession =
        copy(
            phase = FocusPhase.FOCUS,
            timer = FocusTimer.Idle,
            focusesInCycle = if (phase == FocusPhase.LONG_BREAK) 0 else focusesInCycle,
        )

    private fun nextPhaseAfterFocus(completed: Int): FocusPhase =
        if (completed > 0 && completed % FocusPhase.FocusesPerLongBreak == 0) FocusPhase.LONG_BREAK else FocusPhase.SHORT_BREAK
}

sealed interface FocusTimer {
    data object Idle : FocusTimer

    data class Running(val endsAt: Instant) : FocusTimer

    data class Paused(val remaining: Duration) : FocusTimer
}
