package io.github.taetae98coding.jarvis.domain.focus

import kotlinx.coroutines.flow.first

/*
 * 명령은 모두 저장된 상태를 지금 시각으로 푼 뒤(FocusSession.settle) 바꾼다. 앱이 꺼진 동안 끝난 단계를
 * 먼저 넘겨야, 이미 끝난 집중을 일시정지하거나 두 번 세는 일이 없다.
 */
private suspend fun FocusSessionRepository.settled(clock: FocusClock): FocusSession =
    observeFocusSession().first().settle(clock.now(), clock)

/** 멈춰 있는 단계를 처음부터 시작한다. 이미 돌고 있거나 일시정지면 아무것도 하지 않는다. */
class StartFocusTimerUseCase(
    private val repository: FocusSessionRepository,
    private val alarm: FocusAlarmRepository,
    private val clock: FocusClock,
) {
    suspend operator fun invoke() {
        val session = repository.settled(clock)
        if (session.timer != FocusTimer.Idle) return

        val endsAt = clock.now() + session.phase.duration
        repository.saveFocusSession(session.copy(timer = FocusTimer.Running(endsAt)))
        alarm.schedule(endsAt, session.phase)
    }
}

class PauseFocusTimerUseCase(
    private val repository: FocusSessionRepository,
    private val alarm: FocusAlarmRepository,
    private val clock: FocusClock,
) {
    suspend operator fun invoke() {
        val session = repository.settled(clock)
        val timer = session.timer as? FocusTimer.Running ?: return

        repository.saveFocusSession(session.copy(timer = FocusTimer.Paused(timer.endsAt - clock.now())))
        alarm.cancel()
    }
}

class ResumeFocusTimerUseCase(
    private val repository: FocusSessionRepository,
    private val alarm: FocusAlarmRepository,
    private val clock: FocusClock,
) {
    suspend operator fun invoke() {
        val session = repository.settled(clock)
        val timer = session.timer as? FocusTimer.Paused ?: return

        val endsAt = clock.now() + timer.remaining
        repository.saveFocusSession(session.copy(timer = FocusTimer.Running(endsAt)))
        alarm.schedule(endsAt, session.phase)
    }
}

/** 첫 집중으로 돌아간다. 오늘 마친 횟수는 지우지 않는다. */
class ResetFocusTimerUseCase(
    private val repository: FocusSessionRepository,
    private val alarm: FocusAlarmRepository,
    private val clock: FocusClock,
) {
    suspend operator fun invoke() {
        val session = repository.settled(clock)

        repository.saveFocusSession(FocusSession(todayCount = session.todayCount, todayEpochDay = session.todayEpochDay))
        alarm.cancel()
    }
}

class SkipFocusPhaseUseCase(
    private val repository: FocusSessionRepository,
    private val alarm: FocusAlarmRepository,
    private val clock: FocusClock,
) {
    suspend operator fun invoke() {
        repository.saveFocusSession(repository.settled(clock).skipped())
        alarm.cancel()
    }
}
