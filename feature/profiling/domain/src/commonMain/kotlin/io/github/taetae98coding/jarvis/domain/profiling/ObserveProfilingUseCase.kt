package io.github.taetae98coding.jarvis.domain.profiling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveProfilingUseCase(
    private val repository: ProfilingRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<Profiling> =
        repository.observeProfiling()
            .stateIn(scope, SharingStarted.WhileSubscribed(), Profiling.initial(repository.supportedMetrics))
}
