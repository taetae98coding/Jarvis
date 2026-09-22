package io.github.taetae98coding.jarvis.data

import io.github.taetae98coding.jarvis.data.appinfo.DefaultAppInfoRepository
import io.github.taetae98coding.jarvis.data.emulator.DefaultEmulatorRepository
import io.github.taetae98coding.jarvis.data.emulator.emulatorDataSource
import io.github.taetae98coding.jarvis.data.screen.DefaultScreenAwakeRepository
import io.github.taetae98coding.jarvis.data.screen.DefaultScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.data.screen.DefaultSystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.data.screen.createIdleInhibitor
import io.github.taetae98coding.jarvis.data.screen.createSystemScreenAwakeDataSource
import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import kotlinx.coroutines.CoroutineScope

/**
 * 리포지토리 구현을 만들어 내놓는다. 바깥에서 보이는 타입은 전부 도메인 인터페이스다.
 *
 * [scope] 는 구독자가 없어도 살아 있어야 하는 상태(설정값, 시스템 전역 화면 유지 상태)의 수명이다.
 * 이 컨테이너를 만든 쪽이 앱 수명과 같은 스코프를 준다.
 */
class DataModule(
    context: PlatformContext,
    scope: CoroutineScope,
) {
    val appInfoRepository: AppInfoRepository = DefaultAppInfoRepository()

    val emulatorRepository: EmulatorRepository = DefaultEmulatorRepository(emulatorDataSource)

    val screenAwakeSettingsRepository: ScreenAwakeSettingsRepository =
        DefaultScreenAwakeSettingsRepository(createSettingsStore(context), scope)

    val screenAwakeRepository: ScreenAwakeRepository =
        DefaultScreenAwakeRepository(createIdleInhibitor())

    val systemScreenAwakeRepository: SystemScreenAwakeRepository =
        DefaultSystemScreenAwakeRepository(createSystemScreenAwakeDataSource(context), scope)
}
