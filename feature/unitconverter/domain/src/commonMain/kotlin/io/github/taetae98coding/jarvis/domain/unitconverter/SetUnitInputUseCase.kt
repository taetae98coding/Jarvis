package io.github.taetae98coding.jarvis.domain.unitconverter

class SetUnitInputUseCase(
    private val settings: UnitConverterSettingsRepository,
) {
    operator fun invoke(category: UnitCategory, input: String) {
        settings.setInput(category, input)
    }
}
