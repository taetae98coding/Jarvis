package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.browser.browserModule
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.appinfo.appInfoDataModule
import io.github.taetae98coding.jarvis.data.battery.batteryDataModule
import io.github.taetae98coding.jarvis.data.devtools.devToolsDataModule
import io.github.taetae98coding.jarvis.data.unitconverter.unitConverterDataModule
import io.github.taetae98coding.jarvis.data.emulator.emulatorDataModule
import io.github.taetae98coding.jarvis.data.focus.focusDataModule
import io.github.taetae98coding.jarvis.data.profiling.profilingDataModule
import io.github.taetae98coding.jarvis.data.rotation.rotationDataModule
import io.github.taetae98coding.jarvis.data.screen.screenDataModule
import io.github.taetae98coding.jarvis.data.terminal.terminalDataModule
import io.github.taetae98coding.jarvis.data.texttools.textToolsDataModule
import io.github.taetae98coding.jarvis.data.theme.themeDataModule
import io.github.taetae98coding.jarvis.domain.appinfo.appInfoDomainModule
import io.github.taetae98coding.jarvis.domain.battery.batteryDomainModule
import io.github.taetae98coding.jarvis.domain.devtools.devToolsDomainModule
import io.github.taetae98coding.jarvis.domain.unitconverter.unitConverterDomainModule
import io.github.taetae98coding.jarvis.domain.emulator.emulatorDomainModule
import io.github.taetae98coding.jarvis.domain.focus.focusDomainModule
import io.github.taetae98coding.jarvis.domain.mcp.mcpDomainModule
import io.github.taetae98coding.jarvis.domain.profiling.profilingDomainModule
import io.github.taetae98coding.jarvis.domain.rotation.rotationDomainModule
import io.github.taetae98coding.jarvis.domain.screen.screenDomainModule
import io.github.taetae98coding.jarvis.domain.terminal.terminalDomainModule
import io.github.taetae98coding.jarvis.domain.texttools.textToolsDomainModule
import io.github.taetae98coding.jarvis.domain.theme.themeDomainModule
import io.github.taetae98coding.jarvis.ui.appUiModule
import io.github.taetae98coding.jarvis.ui.appinfo.appInfoUiModule
import io.github.taetae98coding.jarvis.ui.battery.batteryUiModule
import io.github.taetae98coding.jarvis.ui.devtools.devToolsUiModule
import io.github.taetae98coding.jarvis.ui.unitconverter.unitConverterUiModule
import io.github.taetae98coding.jarvis.ui.emulator.emulatorUiModule
import io.github.taetae98coding.jarvis.ui.focus.focusUiModule
import io.github.taetae98coding.jarvis.ui.profiling.profilingUiModule
import io.github.taetae98coding.jarvis.ui.rotation.rotationUiModule
import io.github.taetae98coding.jarvis.ui.screen.screenUiModule
import io.github.taetae98coding.jarvis.ui.terminal.terminalUiModule
import io.github.taetae98coding.jarvis.ui.texttools.textToolsUiModule
import io.github.taetae98coding.jarvis.ui.theme.themeUiModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

/**
 * 진입점이 만든 플랫폼 값. 기능 모듈들은 이것을 `get()` 으로만 받는다.
 *
 * 앱 수명 `CoroutineScope` 는 두지 않는다. 기능의 data 모듈이 스스로 구독을 붙잡고 있지 않아야
 * 아무도 보지 않는 상태의 리스너와 폴링이 멈춘다(docs/common/state-observation.html R12).
 */
internal fun platformModule(context: PlatformContext): Module =
    module {
        single { context }
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
        themeDomainModule, themeDataModule, themeUiModule,
        profilingDomainModule, profilingDataModule, profilingUiModule,
        batteryDomainModule, batteryDataModule, batteryUiModule,
        focusDomainModule, focusDataModule, focusUiModule,
        devToolsDomainModule, devToolsDataModule, devToolsUiModule,
        unitConverterDomainModule, unitConverterDataModule, unitConverterUiModule,
        textToolsDomainModule, textToolsDataModule, textToolsUiModule,
        browserModule, mcpDomainModule,
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
