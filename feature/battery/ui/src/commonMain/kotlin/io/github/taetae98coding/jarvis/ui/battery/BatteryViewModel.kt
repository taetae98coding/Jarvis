package io.github.taetae98coding.jarvis.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import io.github.taetae98coding.jarvis.domain.battery.ObserveBatteryUseCase
import kotlinx.coroutines.flow.StateFlow

internal class BatteryViewModel(
    observeBattery: ObserveBatteryUseCase,
) : ViewModel() {
    val battery: StateFlow<BatteryStatus> = observeBattery(viewModelScope)
}
