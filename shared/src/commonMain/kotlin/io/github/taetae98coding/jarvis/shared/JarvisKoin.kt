package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.appinfo.appInfoDataModule
import io.github.taetae98coding.jarvis.data.emulator.emulatorDataModule
import io.github.taetae98coding.jarvis.data.rotation.rotationDataModule
import io.github.taetae98coding.jarvis.data.screen.screenDataModule
import io.github.taetae98coding.jarvis.data.terminal.terminalDataModule
import io.github.taetae98coding.jarvis.domain.appinfo.appInfoDomainModule
import io.github.taetae98coding.jarvis.domain.emulator.emulatorDomainModule
import io.github.taetae98coding.jarvis.domain.rotation.rotationDomainModule
import io.github.taetae98coding.jarvis.domain.screen.screenDomainModule
import io.github.taetae98coding.jarvis.domain.terminal.terminalDomainModule
import io.github.taetae98coding.jarvis.ui.appUiModule
import io.github.taetae98coding.jarvis.ui.appinfo.appInfoUiModule
import io.github.taetae98coding.jarvis.ui.emulator.emulatorUiModule
import io.github.taetae98coding.jarvis.ui.rotation.rotationUiModule
import io.github.taetae98coding.jarvis.ui.screen.screenUiModule
import io.github.taetae98coding.jarvis.ui.terminal.terminalUiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

/**
 * 진입점이 만든 플랫폼 값. 기능 모듈들은 이 둘을 `get()` 으로만 받는다.
 *
 * 앱 수명 스코프의 디스패처가 `Main` 인 이유는 구독자가 없어도 살아 있어야 하는 상태들이 플랫폼
 * API 를 읽기 때문이다. iOS 의 회전·화면 유지는 UIKit 을 부르고 메인 스레드를 요구한다. JVM 에서
 * 이 디스패처는 `kotlinx-coroutines-swing` 이 있어야 존재하는데 Compose Desktop 은 그것을 데려오지
 * 않는다. 그래서 이 모듈의 `jvmMain` 이 직접 선언한다.
 */
internal fun platformModule(context: PlatformContext): Module =
    module {
        single { context }
        single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    }

/**
 * 기능 모듈들이 각자 내놓은 Koin 모듈을 한 줄씩 모은다. 기능을 더할 때 이 목록에 세 줄이 늘고,
 * 기존 코드는 고치지 않는다.
 */
internal fun jarvisModules(context: PlatformContext): List<Module> =
    listOf(
        platformModule(context),
        appInfoDomainModule, appInfoDataModule, appInfoUiModule,
        emulatorDomainModule, emulatorDataModule, emulatorUiModule,
        screenDomainModule, screenDataModule, screenUiModule,
        rotationDomainModule, rotationDataModule, rotationUiModule,
        terminalDomainModule, terminalDataModule, terminalUiModule,
        appUiModule,
    )

/**
 * 프로세스에 Koin 을 한 번만 세운다.
 *
 * 두 번 불릴 수 있다. Android 는 Activity 가 다시 만들어지면 `onCreate` 가 다시 돌고, iOS 는
 * SwiftUI 가 `MainViewController()` 를 다시 만들 수 있다. 이미 시작돼 있으면 아무 일도 하지 않는다 —
 * `startKoin` 을 다시 부르면 `KoinAppAlreadyStartedException` 이다.
 */
internal fun startJarvisKoinWith(context: PlatformContext) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return

    startKoin { modules(jarvisModules(context)) }
}
