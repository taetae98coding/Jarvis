package io.github.taetae98coding.jarvis.domain.calculator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ObserveCalculatorTabUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<CalculatorTab> =
        settings.observeSelectedTab()
            .stateIn(scope, SharingStarted.WhileSubscribed(), settings.readSelectedTab())
}

class SelectCalculatorTabUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    operator fun invoke(tab: CalculatorTab) {
        settings.setSelectedTab(tab)
    }
}

/** 칸 아홉의 저장된 입력. 한 칸이 바뀌면 전체 지도를 다시 흘린다. */
class ObserveCalculatorInputsUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<Map<CalculatorField, String>> {
        val fields = CalculatorField.entries

        return combine(fields.map(settings::observeInput)) { inputs -> fields.zip(inputs).toMap() }
            .stateIn(scope, SharingStarted.WhileSubscribed(), fields.associateWith(settings::readInput))
    }
}

class SetCalculatorInputUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    operator fun invoke(field: CalculatorField, input: String) {
        settings.setInput(field, input)
    }
}
