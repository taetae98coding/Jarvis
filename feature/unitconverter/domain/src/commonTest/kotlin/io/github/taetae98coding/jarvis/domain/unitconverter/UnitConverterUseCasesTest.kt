package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UnitConverterUseCasesTest {
    @Test
    fun stateStartsFromStoredValues() = runTest {
        val settings = FakeSettings()
        settings.setCategory(UnitCategory.AREA)
        settings.setInput(UnitCategory.AREA, "84")

        val state = ObserveUnitConverterStateUseCase(settings)(backgroundScope)

        assertEquals(UnitConverterState(UnitCategory.AREA, UnitSelection(MeasureUnit.PYEONG, MeasureUnit.SQUARE_METER, "84")), state.value)
    }

    @Test
    fun stateFollowsSelectedCategoryAndItsSelection() = runTest {
        val settings = FakeSettings()
        val state = ObserveUnitConverterStateUseCase(settings)(backgroundScope)
        assertEquals(UnitCategory.LENGTH, state.first().category)

        SetUnitInputUseCase(settings)(UnitCategory.TEMPERATURE, "100")
        SelectUnitsUseCase(settings)(MeasureUnit.KELVIN, MeasureUnit.CELSIUS)
        SelectUnitCategoryUseCase(settings)(UnitCategory.TEMPERATURE)

        assertEquals(
            UnitConverterState(UnitCategory.TEMPERATURE, UnitSelection(MeasureUnit.KELVIN, MeasureUnit.CELSIUS, "100")),
            state.first { it.category == UnitCategory.TEMPERATURE },
        )
    }

    @Test
    fun selectionIsKeptPerCategory() {
        val settings = FakeSettings()
        SetUnitInputUseCase(settings)(UnitCategory.LENGTH, "1")
        SetUnitInputUseCase(settings)(UnitCategory.MASS, "2")
        SelectUnitsUseCase(settings)(MeasureUnit.GEUN, MeasureUnit.GRAM)

        assertEquals(UnitSelection(MeasureUnit.CENTIMETER, MeasureUnit.INCH, "1"), settings.readSelection(UnitCategory.LENGTH))
        assertEquals(UnitSelection(MeasureUnit.GEUN, MeasureUnit.GRAM, "2"), settings.readSelection(UnitCategory.MASS))
    }

    @Test
    fun selectUnitsRejectsMixedCategories() {
        assertFailsWith<IllegalArgumentException> { SelectUnitsUseCase(FakeSettings())(MeasureUnit.METER, MeasureUnit.KELVIN) }
    }

    @Test
    fun convertUseCaseUsesConverter() {
        val converted = assertIs<UnitConversion.Converted>(ConvertUnitUseCase()("100", MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT))

        assertEquals("212", converted.result.text)
        assertEquals(UnitCategory.TEMPERATURE.units.size, converted.all.size)
    }
}

private class FakeSettings : UnitConverterSettingsRepository {
    private val category = MutableStateFlow(UnitCategory.LENGTH)
    private val selections = MutableStateFlow(emptyMap<UnitCategory, UnitSelection>())

    override fun observeCategory(): Flow<UnitCategory> = category

    override fun readCategory(): UnitCategory = category.value

    override fun setCategory(category: UnitCategory) {
        this.category.value = category
    }

    override fun observeSelection(category: UnitCategory): Flow<UnitSelection> = selections.map { it.of(category) }

    override fun readSelection(category: UnitCategory): UnitSelection = selections.value.of(category)

    override fun setUnits(category: UnitCategory, from: MeasureUnit, to: MeasureUnit) {
        selections.value += category to readSelection(category).copy(from = from, to = to)
    }

    override fun setInput(category: UnitCategory, input: String) {
        selections.value += category to readSelection(category).copy(input = input)
    }

    private fun Map<UnitCategory, UnitSelection>.of(category: UnitCategory) = this[category] ?: UnitSelection.default(category)
}
