package io.github.taetae98coding.jarvis.data.unitconverter

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConverterSettingsRepository
import org.koin.dsl.module

val unitConverterDataModule = module {
    single<UnitConverterSettingsRepository> { DefaultUnitConverterSettingsRepository(createSettingsStore(get())) }
}
