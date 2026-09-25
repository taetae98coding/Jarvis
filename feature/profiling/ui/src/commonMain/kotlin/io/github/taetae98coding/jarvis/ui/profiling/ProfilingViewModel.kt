package io.github.taetae98coding.jarvis.ui.profiling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.profiling.ObserveProfilingUseCase
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import kotlinx.coroutines.flow.StateFlow

internal class ProfilingViewModel(
    observeProfiling: ObserveProfilingUseCase,
) : ViewModel() {
    val profiling: StateFlow<Profiling> = observeProfiling(viewModelScope)
}
