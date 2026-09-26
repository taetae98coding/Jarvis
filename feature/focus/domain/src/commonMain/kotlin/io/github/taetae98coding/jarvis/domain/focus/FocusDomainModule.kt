package io.github.taetae98coding.jarvis.domain.focus

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val focusDomainModule = module {
    factoryOf(::ObserveFocusTimerUseCase)
    factoryOf(::StartFocusTimerUseCase)
    factoryOf(::PauseFocusTimerUseCase)
    factoryOf(::ResumeFocusTimerUseCase)
    factoryOf(::ResetFocusTimerUseCase)
    factoryOf(::SkipFocusPhaseUseCase)
}
