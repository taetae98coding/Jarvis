package io.github.taetae98coding.jarvis.ui.unitconverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.unitconverter.ConvertUnitUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.ObserveUnitConverterStateUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConversion
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConverterState
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitNumberFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class UnitConverterCardViewModel(
    observeState: ObserveUnitConverterStateUseCase,
    private val convert: ConvertUnitUseCase,
) : ViewModel() {
    private val state = observeState(viewModelScope)

    val summary: StateFlow<UnitConverterSummary?> =
        state.map(::summary).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), summary(state.value))

    private fun summary(state: UnitConverterState): UnitConverterSummary? {
        val selection = state.selection
        val conversion = convert(selection.input, selection.from, selection.to) as? UnitConversion.Converted ?: return null
        val input = UnitNumberFormat.format(conversion.value) ?: return null
        val result = conversion.result.text ?: return null

        return UnitConverterSummary(input = input, from = selection.from, result = result, to = selection.to)
    }
}

/** 카드에 보이는 마지막 변환. 입력이 비었거나 숫자가 아니면 카드는 설명을 보인다. */
internal data class UnitConverterSummary(
    val input: String,
    val from: MeasureUnit,
    val result: String,
    val to: MeasureUnit,
)
