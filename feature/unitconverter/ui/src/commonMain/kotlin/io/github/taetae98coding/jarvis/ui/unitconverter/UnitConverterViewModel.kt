package io.github.taetae98coding.jarvis.ui.unitconverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.unitconverter.ConvertUnitUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.ObserveUnitConverterStateUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.SelectUnitCategoryUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.SelectUnitsUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.SetUnitInputUseCase
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitCategory
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConversion
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConverterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class UnitConverterViewModel(
    observeState: ObserveUnitConverterStateUseCase,
    private val selectCategory: SelectUnitCategoryUseCase,
    private val selectUnits: SelectUnitsUseCase,
    private val setInput: SetUnitInputUseCase,
    private val convert: ConvertUnitUseCase,
) : ViewModel() {
    val state: StateFlow<UnitConverterState> = observeState(viewModelScope)

    // 저장이 신호로 돌아오기 전에도 결과가 입력을 따라가도록, 분류마다 화면이 마지막으로 넘긴 입력을 든다.
    private val drafts = MutableStateFlow(emptyMap<UnitCategory, String>())

    val conversion: StateFlow<UnitConversion> =
        combine(state, drafts, ::conversion)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), conversion(state.value, drafts.value))

    /** 입력 칸을 처음 채울 값. 분류를 오가도 방금 친 글자가 저장값보다 앞선다. */
    fun inputOf(state: UnitConverterState): String = drafts.value[state.category] ?: state.selection.input

    fun onSelectCategory(category: UnitCategory) {
        selectCategory(category)
    }

    fun onSelectFrom(unit: MeasureUnit) {
        selectUnits(unit, state.value.selection.to)
    }

    fun onSelectTo(unit: MeasureUnit) {
        selectUnits(state.value.selection.from, unit)
    }

    fun onSwap() {
        val selection = state.value.selection
        selectUnits(selection.to, selection.from)
    }

    fun onInputChange(category: UnitCategory, input: String) {
        drafts.update { it + (category to input) }
        setInput(category, input)
    }

    private fun conversion(state: UnitConverterState, drafts: Map<UnitCategory, String>): UnitConversion =
        convert(drafts[state.category] ?: state.selection.input, state.selection.from, state.selection.to)
}
