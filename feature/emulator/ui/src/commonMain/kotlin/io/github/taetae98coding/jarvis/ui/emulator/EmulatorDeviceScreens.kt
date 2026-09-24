package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import io.github.taetae98coding.jarvis.ui.device.DeviceScreens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 터미널의 기기 탭에 이 기능의 목록과 화면을 내준다. ViewModel 은 부르는 쪽(터미널 화면)의 백스택 항목에 붙는다.
 */
internal object EmulatorDeviceScreens : DeviceScreens {
    @Composable
    override fun choices(): List<DeviceChoice>? =
        koinViewModel<DeviceChoicesViewModel>().choices.collectAsStateWithLifecycle().value

    // 한 화면에 여러 기기 탭이 함께 보인다. key 가 없으면 모두 처음 만든 기기의 ViewModel 을 받는다.
    @Composable
    override fun Screen(deviceId: String, modifier: Modifier) {
        EmulatorStream(
            viewModel = koinViewModel(key = "device-screen:$deviceId") { parametersOf(deviceId) },
            modifier = modifier,
        )
    }
}
