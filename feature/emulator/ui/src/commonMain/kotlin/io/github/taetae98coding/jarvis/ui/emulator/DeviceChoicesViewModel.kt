package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 터미널 새 탭 메뉴의 기기 구획. 초기값 null 이 "찾는 중" 이라 [EmulatorDevicesViewModel] 의 빈 목록과
 * 따로 둔다. 메뉴가 닫히면 수집이 끝나 폴링도 멈추고, 값은 남아 다음에 열 때 곧바로 보인다.
 */
internal class DeviceChoicesViewModel(
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
) : ViewModel() {
    val choices: StateFlow<List<DeviceChoice>?> =
        observeEmulatorDevices()
            .map { devices ->
                devices.filter { it.canStream }.map { device ->
                    DeviceChoice(
                        id = device.id,
                        name = device.name,
                        kind = when {
                            device.platform == EmulatorPlatform.IOS -> "iOS 시뮬레이터"
                            device.isPhysical -> "Android 실물 기기"
                            else -> "Android 에뮬레이터"
                        },
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}
