package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import org.koin.dsl.module

/**
 * PlatformContext 와 앱 수명 `CoroutineScope` 는 `get()` 으로 받는다. 둘은 진입점이 만들어
 * `:shared` 의 `platformModule` 이 등록한다.
 *
 * 전부 single 이다. 설정값을 들고 있어서 두 개가 생기면 설정 변경이 서로에게 보이지 않는다.
 */
val screenDataModule = module {
    single<ScreenAwakeSettingsRepository> {
        DefaultScreenAwakeSettingsRepository(createSettingsStore(get()), get())
    }

    single<ScreenAwakeRepository> { DefaultScreenAwakeRepository(createIdleInhibitor()) }

    single<SystemScreenAwakeRepository> {
        DefaultSystemScreenAwakeRepository(createSystemScreenAwakeDataSource(get()), get())
    }
}
