package io.github.taetae98coding.jarvis.data.calculator

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorField
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorSettingsRepository
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultCalculatorSettingsRepository(
    private val store: SettingsStore,
) : CalculatorSettingsRepository {
    override fun observeSelectedTab(): Flow<CalculatorTab> =
        store.observeString(SelectedTabKey, CalculatorTab.EXPRESSION.storedValue).map(CalculatorTab::fromStored)

    override fun readSelectedTab(): CalculatorTab =
        CalculatorTab.fromStored(store.getString(SelectedTabKey, CalculatorTab.EXPRESSION.storedValue))

    override fun setSelectedTab(tab: CalculatorTab) {
        store.putString(SelectedTabKey, tab.storedValue)
    }

    override fun observeInput(field: CalculatorField): Flow<String> = store.observeString(inputKey(field), "")

    override fun readInput(field: CalculatorField): String = store.getString(inputKey(field), "")

    override fun setInput(field: CalculatorField, input: String) {
        store.putString(inputKey(field), input)
    }

    override fun observeHistory(): Flow<List<String>> = store.observeString(HistoryKey, "").map(::decodeHistory)

    override fun readHistory(): List<String> = decodeHistory(store.getString(HistoryKey, ""))

    override fun setHistory(expressions: List<String>) {
        store.putString(HistoryKey, expressions.joinToString(HistorySeparator))
    }

    internal companion object {
        const val SelectedTabKey = "calculator_selected_tab"
        const val HistoryKey = "calculator_history"

        // SettingsStore 는 문자열 목록을 담지 못해 한 문자열에 잇는다. 도메인이 수식의 줄바꿈을 공백으로 바꿔 넘기므로 부딪히지 않는다.
        private const val HistorySeparator = "\n"

        fun inputKey(field: CalculatorField): String = "calculator_input_${field.storedValue}"

        private fun decodeHistory(value: String): List<String> = value.split(HistorySeparator).filter { it.isNotBlank() }
    }
}
