package io.github.taetae98coding.jarvis.domain.unitconverter

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val unitConverterDomainModule = module {
    factoryOf(::ObserveUnitConverterStateUseCase)
    factoryOf(::SelectUnitCategoryUseCase)
    factoryOf(::SelectUnitsUseCase)
    factoryOf(::SetUnitInputUseCase)
    factoryOf(::ConvertUnitUseCase)
}
