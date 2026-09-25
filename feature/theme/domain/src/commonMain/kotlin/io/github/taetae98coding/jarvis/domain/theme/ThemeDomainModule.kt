package io.github.taetae98coding.jarvis.domain.theme

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val themeDomainModule = module {
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
    factoryOf(::ApplyThemeModeUseCase)
}
