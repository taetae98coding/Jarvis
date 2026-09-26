package io.github.taetae98coding.jarvis.domain.focus

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** 화면에 그릴 한 순간의 타이머. [remaining] 은 초 단위로 올림한 값이라 1초가 지나야 바뀐다. */
data class FocusTimerState(
    val phase: FocusPhase,
    val status: FocusStatus,
    val remaining: Duration,
    val focusesInCycle: Int,
    val todayCount: Int,
) {
    val total: Duration get() = phase.duration

    /** 지난 비율 0–1. */
    val progress: Float
        get() = (1.0 - remaining / total).coerceIn(0.0, 1.0).toFloat()

    companion object {
        fun of(session: FocusSession, now: Instant, clock: FocusClock): FocusTimerState {
            val settled = session.settle(now, clock)
            val (status, remaining) = when (val timer = settled.timer) {
                FocusTimer.Idle -> FocusStatus.IDLE to settled.phase.duration
                is FocusTimer.Paused -> FocusStatus.PAUSED to timer.remaining
                is FocusTimer.Running -> FocusStatus.RUNNING to (timer.endsAt - now)
            }

            return FocusTimerState(
                phase = settled.phase,
                status = status,
                remaining = remaining.ceilToSeconds(),
                focusesInCycle = settled.focusesInCycle,
                todayCount = settled.todayCount(clock.localEpochDay(now)),
            )
        }
    }
}

enum class FocusStatus {
    IDLE,
    RUNNING,
    PAUSED,
}

private fun Duration.ceilToSeconds(): Duration {
    val whole = inWholeSeconds
    return (if (this > whole.seconds) whole + 1 else whole).coerceAtLeast(0).seconds
}
