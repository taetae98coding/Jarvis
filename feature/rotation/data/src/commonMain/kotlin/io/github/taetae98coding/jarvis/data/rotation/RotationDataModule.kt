package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import org.koin.dsl.module

val rotationDataModule = module {
    single<DeviceRotationRepository> {
        DefaultDeviceRotationRepository(createDeviceRotationDataSource(get()), get())
    }
}
