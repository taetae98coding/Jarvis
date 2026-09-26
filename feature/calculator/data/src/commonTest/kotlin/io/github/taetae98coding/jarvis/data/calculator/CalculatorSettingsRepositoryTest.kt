package io.github.taetae98coding.jarvis.data.calculator

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorField
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorSettingsRepositoryTest {
    @Test
    fun defaultsToExpressionTabEmptyInputsAndNoHistory() {
        val repository = DefaultCalculatorSettingsRepository(InMemorySettingsStore())

        assertEquals(CalculatorTab.EXPRESSION, repository.readSelectedTab())
        assertEquals("", repository.readInput(CalculatorField.BMI_HEIGHT))
        assertEquals(emptyList(), repository.readHistory())
    }

    @Test
    fun selectedTabSurvivesNewRepository() {
        val store = InMemorySettingsStore()
        DefaultCalculatorSettingsRepository(store).setSelectedTab(CalculatorTab.BMI)

        assertEquals(CalculatorTab.BMI, DefaultCalculatorSettingsRepository(store).readSelectedTab())
        assertEquals("bmi", store.getString(DefaultCalculatorSettingsRepository.SelectedTabKey, ""))
    }

    @Test
    fun unknownStoredTabFallsBack() {
        val store = InMemorySettingsStore(mutableMapOf<String, Any>(DefaultCalculatorSettingsRepository.SelectedTabKey to "removed"))

        assertEquals(CalculatorTab.EXPRESSION, DefaultCalculatorSettingsRepository(store).readSelectedTab())
    }

    @Test
    fun inputIsKeptPerField() {
        val store = InMemorySettingsStore()
        val repository = DefaultCalculatorSettingsRepository(store)
        repository.setInput(CalculatorField.EXPRESSION, "12×(3+4)")
        repository.setInput(CalculatorField.CHANGE_TO, "80")

        val reopened = DefaultCalculatorSettingsRepository(store)
        assertEquals("12×(3+4)", reopened.readInput(CalculatorField.EXPRESSION))
        assertEquals("80", reopened.readInput(CalculatorField.CHANGE_TO))
        assertEquals("", reopened.readInput(CalculatorField.CHANGE_FROM))
        assertEquals("80", store.getString("calculator_input_change_to", ""))
    }

    @Test
    fun historyRoundTripsInOrder() {
        val store = InMemorySettingsStore()
        DefaultCalculatorSettingsRepository(store).setHistory(listOf("1 + 2", "3×4", "2^10"))

        assertEquals(listOf("1 + 2", "3×4", "2^10"), DefaultCalculatorSettingsRepository(store).readHistory())
    }

    @Test
    fun emptyHistoryAndBlankLinesAreDropped() {
        val store = InMemorySettingsStore(mutableMapOf<String, Any>(DefaultCalculatorSettingsRepository.HistoryKey to "1+1\n\n  \n2+2\n"))
        val repository = DefaultCalculatorSettingsRepository(store)

        assertEquals(listOf("1+1", "2+2"), repository.readHistory())
        repository.setHistory(emptyList())
        assertEquals(emptyList(), repository.readHistory())
    }

    @Test
    fun observeFollowsWrites() = runTest {
        val repository = DefaultCalculatorSettingsRepository(InMemorySettingsStore())

        assertEquals(CalculatorTab.EXPRESSION, repository.observeSelectedTab().first())
        repository.setSelectedTab(CalculatorTab.PERCENT)
        assertEquals(CalculatorTab.PERCENT, repository.observeSelectedTab().first())

        repository.setInput(CalculatorField.BMI_WEIGHT, "70")
        assertEquals("70", repository.observeInput(CalculatorField.BMI_WEIGHT).first())

        repository.setHistory(listOf("1+1"))
        assertEquals(listOf("1+1"), repository.observeHistory().first())
    }
}
