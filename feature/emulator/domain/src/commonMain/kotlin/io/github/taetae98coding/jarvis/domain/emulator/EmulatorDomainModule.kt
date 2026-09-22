package io.github.taetae98coding.jarvis.domain.emulator

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val emulatorDomainModule = module {
    factoryOf(::ObserveEmulatorStatusUseCase)
    factoryOf(::ObserveEmulatorDevicesUseCase)
    factoryOf(::ObserveEmulatorScreenUseCase)
    factoryOf(::SendEmulatorGestureUseCase)
    factoryOf(::LaunchEmulatorUseCase)
    factoryOf(::WakeDeviceUseCase)
}
