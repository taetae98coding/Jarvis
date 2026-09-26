package io.github.taetae98coding.jarvis.data.unitconverter

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitCategory
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitSelection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitConverterSettingsRepositoryTest {
    @Test
    fun defaultsToLengthAndCategoryDefaults() {
        val repository = DefaultUnitConverterSettingsRepository(InMemorySettingsStore())

        assertEquals(UnitCategory.LENGTH, repository.readCategory())
        assertEquals(UnitSelection(MeasureUnit.PYEONG, MeasureUnit.SQUARE_METER, ""), repository.readSelection(UnitCategory.AREA))
    }

    @Test
    fun categoryUnitsAndInputSurviveNewRepository() {
        val store = InMemorySettingsStore()
        DefaultUnitConverterSettingsRepository(store).apply {
            setCategory(UnitCategory.TEMPERATURE)
            setUnits(UnitCategory.TEMPERATURE, MeasureUnit.FAHRENHEIT, MeasureUnit.KELVIN)
            setInput(UnitCategory.TEMPERATURE, "98.6")
            setInput(UnitCategory.AREA, "84")
        }

        val reopened = DefaultUnitConverterSettingsRepository(store)
        assertEquals(UnitCategory.TEMPERATURE, reopened.readCategory())
        assertEquals(UnitSelection(MeasureUnit.FAHRENHEIT, MeasureUnit.KELVIN, "98.6"), reopened.readSelection(UnitCategory.TEMPERATURE))
        assertEquals(UnitSelection(MeasureUnit.PYEONG, MeasureUnit.SQUARE_METER, "84"), reopened.readSelection(UnitCategory.AREA))
        assertEquals("temperature", store.getString(DefaultUnitConverterSettingsRepository.CategoryKey, ""))
        assertEquals("fahrenheit", store.getString(DefaultUnitConverterSettingsRepository.fromKey(UnitCategory.TEMPERATURE), ""))
    }

    @Test
    fun unknownOrForeignStoredValuesFallBack() {
        val store = InMemorySettingsStore(
            mutableMapOf<String, Any>(
                DefaultUnitConverterSettingsRepository.CategoryKey to "removed",
                // 다른 분류의 단위가 적혀 있어도 길이 화면에 넓이 단위를 보이지 않는다.
                DefaultUnitConverterSettingsRepository.fromKey(UnitCategory.LENGTH) to "pyeong",
                DefaultUnitConverterSettingsRepository.toKey(UnitCategory.LENGTH) to "furlong",
            ),
        )
        val repository = DefaultUnitConverterSettingsRepository(store)

        assertEquals(UnitCategory.LENGTH, repository.readCategory())
        assertEquals(UnitSelection(MeasureUnit.CENTIMETER, MeasureUnit.INCH, ""), repository.readSelection(UnitCategory.LENGTH))
    }

    @Test
    fun observeFollowsWrites() = runTest {
        val repository = DefaultUnitConverterSettingsRepository(InMemorySettingsStore())

        assertEquals(UnitCategory.LENGTH, repository.observeCategory().first())
        repository.setCategory(UnitCategory.DATA)
        assertEquals(UnitCategory.DATA, repository.observeCategory().first())

        repository.setUnits(UnitCategory.DATA, MeasureUnit.TEBIBYTE, MeasureUnit.BYTE)
        repository.setInput(UnitCategory.DATA, "1")
        assertEquals(UnitSelection(MeasureUnit.TEBIBYTE, MeasureUnit.BYTE, "1"), repository.observeSelection(UnitCategory.DATA).first())
    }

    @Test
    fun observingSelectionHoldsOneListener() = runTest {
        val store = InMemorySettingsStore()
        val repository = DefaultUnitConverterSettingsRepository(store)
        val seen = mutableListOf<UnitSelection>()

        val job = launch { repository.observeSelection(UnitCategory.MASS).collect { seen += it } }
        testScheduler.runCurrent()
        assertEquals(1, store.listeners)

        repository.setInput(UnitCategory.MASS, "3")
        testScheduler.runCurrent()
        assertEquals("3", seen.last().input)

        job.cancel()
        testScheduler.runCurrent()
        assertEquals(0, store.listeners)
    }
}
