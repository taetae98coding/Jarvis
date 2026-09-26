package io.github.taetae98coding.jarvis.domain.calculator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorUseCasesTest {
    @Test
    fun addPutsNewestFirstAndMovesDuplicatesUp() = runTest {
        val settings = FakeCalculatorSettingsRepository()
        val add = AddCalculationHistoryUseCase(settings)

        add("1+1")
        add("2*3")
        add("1+1")

        assertEquals(listOf("1+1", "2*3"), settings.history.value)
    }

    @Test
    fun addCollapsesWhitespace() = runTest {
        val settings = FakeCalculatorSettingsRepository()

        AddCalculationHistoryUseCase(settings)("  1 \t+\n 2  ")

        assertEquals(listOf("1 + 2"), settings.history.value)
    }

    @Test
    fun addIgnoresExpressionsWithoutAnswer() = runTest {
        val settings = FakeCalculatorSettingsRepository()
        val add = AddCalculationHistoryUseCase(settings)

        add("")
        add("3+")
        add("1/0")

        assertEquals(emptyList(), settings.history.value)
    }

    @Test
    fun addKeepsAtMostTwentyEntries() = runTest {
        val settings = FakeCalculatorSettingsRepository()
        val add = AddCalculationHistoryUseCase(settings)

        repeat(CalculationHistory.MaxEntries + 5) { add("$it+0") }

        assertEquals(CalculationHistory.MaxEntries, settings.history.value.size)
        assertEquals("24+0", settings.history.value.first())
        assertEquals("5+0", settings.history.value.last())
    }

    @Test
    fun clearEmptiesHistory() {
        val settings = FakeCalculatorSettingsRepository(history = listOf("1+1"))

        ClearCalculationHistoryUseCase(settings)()

        assertEquals(emptyList(), settings.history.value)
    }

    @Test
    fun observedHistoryRecomputesAnswersAndDropsBrokenLines() = runTest {
        val settings = FakeCalculatorSettingsRepository(history = listOf("0.1+0.2", "garbage(", "2^10"))
        val history = ObserveCalculationHistoryUseCase(settings)(backgroundScope)

        val expected = listOf(CalculationHistoryEntry("0.1+0.2", "0.3"), CalculationHistoryEntry("2^10", "1024"))
        assertEquals(expected, history.value)
        assertEquals(expected, history.first())
    }

    @Test
    fun tabFollowsSelection() = runTest {
        val settings = FakeCalculatorSettingsRepository()
        val tab = ObserveCalculatorTabUseCase(settings)(backgroundScope)

        assertEquals(CalculatorTab.EXPRESSION, tab.value)
        SelectCalculatorTabUseCase(settings)(CalculatorTab.BMI)
        assertEquals(CalculatorTab.BMI, tab.first { it == CalculatorTab.BMI })
    }

    @Test
    fun inputsStartFromSavedValuesAndFollowWrites() = runTest {
        val settings = FakeCalculatorSettingsRepository().apply { inputs.value = mapOf(CalculatorField.BMI_HEIGHT to "170") }
        val inputs = ObserveCalculatorInputsUseCase(settings)(backgroundScope)

        assertEquals("170", inputs.value[CalculatorField.BMI_HEIGHT])
        assertEquals("", inputs.value[CalculatorField.EXPRESSION])
        assertEquals(CalculatorField.entries.toSet(), inputs.value.keys)

        SetCalculatorInputUseCase(settings)(CalculatorField.EXPRESSION, "1+2")
        assertEquals("1+2", inputs.first { it[CalculatorField.EXPRESSION] == "1+2" }[CalculatorField.EXPRESSION])
    }

    @Test
    fun calculateUseCasesDelegate() {
        assertEquals(CalculationResult.Value(3.0, "3"), EvaluateExpressionUseCase()("1+2"))
        assertEquals(PercentResult.Value(30.0, "30"), CalculatePercentUseCase()(PercentMode.PERCENT_OF, "200", "15"))
        assertEquals(BmiCategory.NORMAL, (CalculateBmiUseCase()("175", "70") as BmiResult.Value).category)
    }
}

private class FakeCalculatorSettingsRepository(
    history: List<String> = emptyList(),
) : CalculatorSettingsRepository {
    val tab = MutableStateFlow(CalculatorTab.EXPRESSION)
    val inputs = MutableStateFlow(emptyMap<CalculatorField, String>())
    val history = MutableStateFlow(history)

    override fun observeSelectedTab() = tab

    override fun readSelectedTab() = tab.value

    override fun setSelectedTab(tab: CalculatorTab) {
        this.tab.value = tab
    }

    override fun observeInput(field: CalculatorField) = inputs.map { it[field].orEmpty() }

    override fun readInput(field: CalculatorField) = inputs.value[field].orEmpty()

    override fun setInput(field: CalculatorField, input: String) {
        inputs.value += field to input
    }

    override fun observeHistory() = history

    override fun readHistory() = history.value

    override fun setHistory(expressions: List<String>) {
        history.value = expressions
    }
}
