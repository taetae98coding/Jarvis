package io.github.taetae98coding.jarvis.domain.worldclock

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val worldClockDomainModule = module {
    factoryOf(::ObserveWorldClockUseCase)
    factoryOf(::AddCityUseCase)
    factoryOf(::RemoveCityUseCase)
    factoryOf(::SearchCitiesUseCase)
    factoryOf(::ConvertTimeUseCase)
    factoryOf(::CalculateDatesUseCase)
}
