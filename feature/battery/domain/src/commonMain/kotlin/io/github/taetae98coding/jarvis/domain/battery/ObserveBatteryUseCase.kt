package io.github.taetae98coding.jarvis.domain.battery

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ObserveBatteryUseCase(
    private val repository: BatteryRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<BatteryStatus> =
        repository.observeBattery()
            .stateIn(scope, SharingStarted.WhileSubscribed(), BatteryStatus.Loading)
}
