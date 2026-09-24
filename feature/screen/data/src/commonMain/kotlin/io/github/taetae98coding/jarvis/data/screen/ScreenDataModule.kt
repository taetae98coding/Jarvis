package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.notification.createNotificationPermission
import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import org.koin.dsl.module

/**
 * PlatformContext 는 `get()` 으로 받는다. 진입점이 만들어 `:shared` 의 `platformModule` 이 등록한다.
 */
val screenDataModule = module {
    single<ScreenAwakeSettingsRepository> {
        DefaultScreenAwakeSettingsRepository(createSettingsStore(get()))
    }

    single<ScreenAwakeRepository> { DefaultScreenAwakeRepository(createIdleInhibitor()) }

    single<SystemScreenAwakeRepository> {
        DefaultSystemScreenAwakeRepository(createSystemScreenAwakeDataSource(get()))
    }

    single<SystemScreenAwakeNotificationRepository> {
        DefaultSystemScreenAwakeNotificationRepository(createSettingsStore(get()), createNotificationPermission(get()))
    }
}
