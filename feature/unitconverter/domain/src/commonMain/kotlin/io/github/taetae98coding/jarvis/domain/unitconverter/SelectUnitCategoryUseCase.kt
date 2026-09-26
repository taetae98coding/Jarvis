package io.github.taetae98coding.jarvis.domain.unitconverter

class SelectUnitCategoryUseCase(
    private val settings: UnitConverterSettingsRepository,
) {
    operator fun invoke(category: UnitCategory) {
        settings.setCategory(category)
    }
}
