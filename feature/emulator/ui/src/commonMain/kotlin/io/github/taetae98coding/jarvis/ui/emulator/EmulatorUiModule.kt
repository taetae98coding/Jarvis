package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.navKeySerializers
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

/**
 * 이 기능의 ViewModel 과 화면을 등록한다. 앱 셸은 화면이 몇 개인지 모른다.
 *
 * `navigation` DSL 과 `koinEntryProvider()` 는 아직 실험 API 다. 이 조합이 깨졌을 때의 대안은
 * docs/common/navigation.html#deps 에 적어 두었다.
 */
@OptIn(KoinExperimentalAPI::class)
val emulatorUiModule = module {
    viewModelOf(::EmulatorStatusViewModel)
    viewModelOf(::EmulatorDevicesViewModel)
    viewModelOf(::WifiPairingViewModel)

    // 라우트가 나르는 deviceId 를 파라미터로 받는다.
    viewModel { parameters -> EmulatorScreenViewModel(parameters.get(), get(), get(), get(), get()) }

    navigation<EmulatorRoute.Devices> {
        val navigator = LocalNavigator.current

        EmulatorListScreen(
            viewModel = koinViewModel(),
            onSelect = { device -> navigator.goTo(EmulatorRoute.Screen(device.id)) },
            onPair = { navigator.goTo(EmulatorRoute.Pairing) },
            onBack = navigator::back,
        )
    }

    navigation<EmulatorRoute.Screen> { route ->
        val navigator = LocalNavigator.current

        EmulatorStreamScreen(
            viewModel = koinViewModel { parametersOf(route.deviceId) },
            onBack = navigator::back,
        )
    }

    navigation<EmulatorRoute.Pairing> {
        WifiPairingScreen(viewModel = koinViewModel(), onBack = LocalNavigator.current::back)
    }

    navKeySerializers<EmulatorRoute> {
        subclass(EmulatorRoute.Devices::class, EmulatorRoute.Devices.serializer())
        subclass(EmulatorRoute.Screen::class, EmulatorRoute.Screen.serializer())
        subclass(EmulatorRoute.Pairing::class, EmulatorRoute.Pairing.serializer())
    }
}
