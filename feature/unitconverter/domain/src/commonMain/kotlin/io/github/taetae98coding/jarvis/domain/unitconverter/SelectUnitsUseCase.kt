package io.github.taetae98coding.jarvis.domain.unitconverter

class SelectUnitsUseCase(
    private val settings: UnitConverterSettingsRepository,
) {
    operator fun invoke(from: MeasureUnit, to: MeasureUnit) {
        require(from.category == to.category) { "$from 과 $to 는 분류가 다르다" }
        settings.setUnits(from.category, from, to)
    }
}
