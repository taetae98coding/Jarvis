package io.github.taetae98coding.jarvis.domain.rotation

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val rotationDomainModule = module {
    factoryOf(::ObserveDeviceRotationStatusUseCase)
    factoryOf(::SetDeviceRotationAngleUseCase)
    factoryOf(::SetDeviceRotationLockUseCase)
    factoryOf(::RotateDeviceUseCase)
    factoryOf(::ObserveDeviceRotationNotificationUseCase)
    factoryOf(::SetDeviceRotationNotificationPinnedUseCase)
}
