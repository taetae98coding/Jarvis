package io.github.taetae98coding.jarvis.data.calculator

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorSettingsRepository
import org.koin.dsl.module

val calculatorDataModule = module {
    single<CalculatorSettingsRepository> { DefaultCalculatorSettingsRepository(createSettingsStore(get())) }
}
