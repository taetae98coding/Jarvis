package io.github.taetae98coding.jarvis.domain.screen

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val screenDomainModule = module {
    factoryOf(::ObserveKeepScreenAwakeUseCase)
    factoryOf(::ObserveKeepSystemScreenAwakeUseCase)
    factoryOf(::ObserveSystemScreenAwakeStatusUseCase)
    factoryOf(::SetKeepScreenAwakeUseCase)
    factoryOf(::SetKeepSystemScreenAwakeUseCase)
    factoryOf(::ApplyKeepScreenAwakeUseCase)
    factoryOf(::ApplySystemScreenAwakeUseCase)
    factoryOf(::SyncSystemScreenAwakeUseCase)
    factoryOf(::ObserveSystemScreenAwakeNotificationUseCase)
    factoryOf(::SetSystemScreenAwakeNotificationPinnedUseCase)
}
