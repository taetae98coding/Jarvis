package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import io.github.taetae98coding.jarvis.ui.device.DeviceChoicePlatform
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 터미널 새 탭 메뉴의 기기 구획과 실행 창의 기기 목록. 초기값 null 이 "찾는 중" 이라 [EmulatorDevicesViewModel] 의 빈 목록과
 * 따로 둔다. 메뉴·창이 닫히면 수집이 끝나 폴링도 멈추고, 값은 남아 다음에 열 때 곧바로 보인다.
 */
internal class DeviceChoicesViewModel(
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
) : ViewModel() {
    private val devices = observeEmulatorDevices()

    val choices: StateFlow<List<DeviceChoice>?> =
        devices
            .map { devices -> devices.filter { it.canStream }.map { it.toChoice() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /** 꺼진 가상 기기와 실물 iOS 까지 전부(docs/common/terminal-run.html R7·R12). */
    val runTargets: StateFlow<List<DeviceChoice>?> =
        devices
            .map { devices -> devices.map { it.toChoice() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}

private fun EmulatorDevice.toChoice(): DeviceChoice {
    val kind = when {
        platform == EmulatorPlatform.IOS && isPhysical -> listOfNotNull("iOS 실물 기기", connection?.label).joinToString(" · ")
        platform == EmulatorPlatform.IOS -> "iOS 시뮬레이터"
        // 한 기기를 USB 와 무선 디버깅으로 함께 붙이면 이름이 같은 줄이 둘이라 연결 방식으로 가른다.
        isPhysical -> listOfNotNull("Android 실물 기기", connection?.label).joinToString(" · ")
        else -> "Android 에뮬레이터"
    }

    return DeviceChoice(
        id = id,
        name = name,
        kind = if (isRunning) kind else "$kind · 꺼짐",
        platform = if (platform == EmulatorPlatform.IOS) DeviceChoicePlatform.IOS else DeviceChoicePlatform.Android,
        isRunning = isRunning,
        isPhysical = isPhysical,
        // 실물 iOS 는 화면을 찍을 공개 도구가 없다(docs/common/terminal-device.html R1).
        canMirror = !(platform == EmulatorPlatform.IOS && isPhysical),
    )
}
