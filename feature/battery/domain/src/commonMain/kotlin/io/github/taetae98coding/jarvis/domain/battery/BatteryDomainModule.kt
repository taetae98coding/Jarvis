package io.github.taetae98coding.jarvis.domain.battery

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val batteryDomainModule = module {
    factoryOf(::ObserveBatteryUseCase)
}
