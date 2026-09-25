package io.github.taetae98coding.jarvis.domain.profiling

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val profilingDomainModule = module {
    factoryOf(::ObserveProfilingUseCase)
}
