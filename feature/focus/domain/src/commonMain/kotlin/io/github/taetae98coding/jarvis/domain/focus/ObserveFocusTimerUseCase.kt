package io.github.taetae98coding.jarvis.domain.focus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ObserveFocusTimerUseCase(
    private val repository: FocusSessionRepository,
    private val clock: FocusClock,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<FocusTimerState> =
        combine(repository.observeFocusSession(), clock.observeNow()) { session, now ->
            FocusTimerState.of(session, now, clock)
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            FocusTimerState.of(repository.readFocusSession(), clock.now(), clock),
        )
}
