package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ObserveUnitConverterStateUseCase(
    private val settings: UnitConverterSettingsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(scope: CoroutineScope): StateFlow<UnitConverterState> {
        val category = settings.readCategory()

        return settings.observeCategory()
            .flatMapLatest { selected -> settings.observeSelection(selected).map { UnitConverterState(selected, it) } }
            .stateIn(scope, SharingStarted.WhileSubscribed(), UnitConverterState(category, settings.readSelection(category)))
    }
}
