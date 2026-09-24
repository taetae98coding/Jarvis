package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.notification.createNotificationPermission
import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import org.koin.dsl.module

val rotationDataModule = module {
    single<DeviceRotationRepository> {
        DefaultDeviceRotationRepository(createDeviceRotationDataSource(get()))
    }

    single<DeviceRotationNotificationRepository> {
        DefaultDeviceRotationNotificationRepository(createSettingsStore(get()), createNotificationPermission(get()))
    }
}
