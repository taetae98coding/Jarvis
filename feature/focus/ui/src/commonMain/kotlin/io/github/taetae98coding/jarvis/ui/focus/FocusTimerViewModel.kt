package io.github.taetae98coding.jarvis.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.focus.FocusTimerState
import io.github.taetae98coding.jarvis.domain.focus.ObserveFocusTimerUseCase
import io.github.taetae98coding.jarvis.domain.focus.PauseFocusTimerUseCase
import io.github.taetae98coding.jarvis.domain.focus.ResetFocusTimerUseCase
import io.github.taetae98coding.jarvis.domain.focus.ResumeFocusTimerUseCase
import io.github.taetae98coding.jarvis.domain.focus.SkipFocusPhaseUseCase
import io.github.taetae98coding.jarvis.domain.focus.StartFocusTimerUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class FocusTimerViewModel(
    observeFocusTimer: ObserveFocusTimerUseCase,
    private val startFocusTimer: StartFocusTimerUseCase,
    private val pauseFocusTimer: PauseFocusTimerUseCase,
    private val resumeFocusTimer: ResumeFocusTimerUseCase,
    private val resetFocusTimer: ResetFocusTimerUseCase,
    private val skipFocusPhase: SkipFocusPhaseUseCase,
) : ViewModel() {
    val timer: StateFlow<FocusTimerState> = observeFocusTimer(viewModelScope)

    fun onStart() {
        viewModelScope.launch { startFocusTimer() }
    }

    fun onPause() {
        viewModelScope.launch { pauseFocusTimer() }
    }

    fun onResume() {
        viewModelScope.launch { resumeFocusTimer() }
    }

    fun onReset() {
        viewModelScope.launch { resetFocusTimer() }
    }

    fun onSkip() {
        viewModelScope.launch { skipFocusPhase() }
    }
}
