package io.github.taetae98coding.jarvis.domain.terminal

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val terminalDomainModule = module {
    factoryOf(::IsTerminalSupportedUseCase)
    factoryOf(::OpenTerminalSessionUseCase)
}
