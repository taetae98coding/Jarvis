package io.github.taetae98coding.jarvis.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.calculator.AddCalculationHistoryUseCase
import io.github.taetae98coding.jarvis.domain.calculator.BmiResult
import io.github.taetae98coding.jarvis.domain.calculator.CalculateBmiUseCase
import io.github.taetae98coding.jarvis.domain.calculator.CalculatePercentUseCase
import io.github.taetae98coding.jarvis.domain.calculator.CalculationHistoryEntry
import io.github.taetae98coding.jarvis.domain.calculator.CalculationResult
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorField
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import io.github.taetae98coding.jarvis.domain.calculator.ClearCalculationHistoryUseCase
import io.github.taetae98coding.jarvis.domain.calculator.EvaluateExpressionUseCase
import io.github.taetae98coding.jarvis.domain.calculator.ObserveCalculationHistoryUseCase
import io.github.taetae98coding.jarvis.domain.calculator.ObserveCalculatorInputsUseCase
import io.github.taetae98coding.jarvis.domain.calculator.ObserveCalculatorTabUseCase
import io.github.taetae98coding.jarvis.domain.calculator.PercentMode
import io.github.taetae98coding.jarvis.domain.calculator.PercentResult
import io.github.taetae98coding.jarvis.domain.calculator.SelectCalculatorTabUseCase
import io.github.taetae98coding.jarvis.domain.calculator.SetCalculatorInputUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CalculatorViewModel(
    observeTab: ObserveCalculatorTabUseCase,
    observeInputs: ObserveCalculatorInputsUseCase,
    observeHistory: ObserveCalculationHistoryUseCase,
    private val selectTab: SelectCalculatorTabUseCase,
    private val setInput: SetCalculatorInputUseCase,
    private val evaluate: EvaluateExpressionUseCase,
    private val calculatePercent: CalculatePercentUseCase,
    private val calculateBmi: CalculateBmiUseCase,
    private val addHistory: AddCalculationHistoryUseCase,
    private val clearHistory: ClearCalculationHistoryUseCase,
) : ViewModel() {
    val tab: StateFlow<CalculatorTab> = observeTab(viewModelScope)

    val history: StateFlow<List<CalculationHistoryEntry>> = observeHistory(viewModelScope)

    private val savedInputs = observeInputs(viewModelScope)

    // 저장이 신호로 돌아오기 전에도 답이 입력을 따라가도록, 칸마다 화면이 마지막으로 넘긴 입력을 든다.
    private val drafts = MutableStateFlow(emptyMap<CalculatorField, String>())

    val results: StateFlow<CalculatorResults> =
        combine(savedInputs, drafts) { saved, drafts -> results(saved + drafts) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), results(savedInputs.value + drafts.value))

    /** 입력 칸을 처음 채울 값. 탭을 오가도 방금 친 글자가 저장값보다 앞선다. */
    fun inputOf(field: CalculatorField): String = drafts.value[field] ?: savedInputs.value[field].orEmpty()

    fun onSelectTab(tab: CalculatorTab) {
        selectTab(tab)
    }

    fun onInputChange(field: CalculatorField, input: String) {
        // 입력 칸은 처음 채워질 때도 한 번 알린다. 같은 값을 다시 적지 않는다.
        if (inputOf(field) == input) return
        drafts.update { it + (field to input) }
        setInput(field, input)
    }

    /** 답이 있으면 기록에 넣고 입력 칸에 넣을 답 글자를, 없으면 null 을 준다. */
    fun onEquals(expression: String): String? {
        val result = evaluate(expression) as? CalculationResult.Value ?: return null
        viewModelScope.launch { addHistory(expression) }
        return result.text
    }

    fun onClearHistory() {
        clearHistory()
    }

    private fun results(inputs: Map<CalculatorField, String>): CalculatorResults =
        calculatorResults(inputs, evaluate, calculatePercent, calculateBmi)
}

internal data class CalculatorResults(
    val expression: CalculationResult,
    val percent: Map<PercentMode, PercentResult>,
    val bmi: BmiResult,
)

internal fun calculatorResults(
    inputs: Map<CalculatorField, String>,
    evaluate: EvaluateExpressionUseCase,
    calculatePercent: CalculatePercentUseCase,
    calculateBmi: CalculateBmiUseCase,
): CalculatorResults {
    fun input(field: CalculatorField) = inputs[field].orEmpty()

    return CalculatorResults(
        expression = evaluate(input(CalculatorField.EXPRESSION)),
        percent = PercentMode.entries.associateWith { mode ->
            val (first, second) = mode.fields
            calculatePercent(mode, input(first), input(second))
        },
        bmi = calculateBmi(input(CalculatorField.BMI_HEIGHT), input(CalculatorField.BMI_WEIGHT)),
    )
}

internal val PercentMode.fields: Pair<CalculatorField, CalculatorField>
    get() = when (this) {
        PercentMode.PERCENT_OF -> CalculatorField.PERCENT_OF_BASE to CalculatorField.PERCENT_OF_RATE
        PercentMode.RATIO -> CalculatorField.RATIO_PART to CalculatorField.RATIO_WHOLE
        PercentMode.CHANGE -> CalculatorField.CHANGE_FROM to CalculatorField.CHANGE_TO
    }
