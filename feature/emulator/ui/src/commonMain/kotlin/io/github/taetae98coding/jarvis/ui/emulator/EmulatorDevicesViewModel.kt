package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.LaunchEmulatorUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * 기기 목록 화면의 상태. 화면이 백스택에서 빠지면 폴링도 함께 끝난다.
 */
internal class EmulatorDevicesViewModel(
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
    private val launchEmulator: LaunchEmulatorUseCase,
    private val wakeDevice: WakeDeviceUseCase,
) : ViewModel() {
    // 목록은 화면이 보고 있는 동안에만 센다. 실행 중인 기기 수만큼 명령이 더 도는 조회라, 전환
    // 애니메이션이 끝난 뒤까지 돌릴 이유가 없다.
    val devices: StateFlow<List<EmulatorDevice>> =
        observeEmulatorDevices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // 실행을 누른 기기. 여기 남아 있다고 해서 아직 뜨지 않았다는 뜻은 아니다.
    private val launchRequests = MutableStateFlow(emptySet<String>())

    /**
     * 실행을 눌렀고 아직 뜨지 않은 기기. 뜨는 데 수십 초가 걸리는 동안 버튼을 잠가 둔다.
     *
     * 값을 목록에서 유도한다. 켜졌다는 신호를 코루틴이 받아서 집합을 고치게 두면, 그 코루틴이
     * 전이를 한 번 놓쳤을 때 버튼이 [LaunchTimeout] 동안 잠긴 채로 남는다. 목록이 곧 사실이므로
     * 목록에서 그 기기가 꺼진 채로 보이지 않으면 잠금도 풀린다.
     */
    val launchingDevices: StateFlow<Set<String>> =
        combine(launchRequests, devices) { requested, devices ->
            requested.filterTo(mutableSetOf()) { id -> devices.any { it.id == id && !it.isRunning } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    fun onLaunch(device: EmulatorDevice) {
        if (!device.canLaunch || device.id in launchRequests.value) return

        launchRequests.value += device.id

        viewModelScope.launch {
            try {
                launchEmulator(device)

                // 뜨지 않는 기기를 영원히 잠가 두지 않기 위한 대비일 뿐이다. 켜졌을 때 잠금을 푸는
                // 것은 launchingDevices 가 목록을 보고 한다.
                delay(LaunchTimeout)
            } finally {
                launchRequests.value -= device.id
            }
        }
    }

    // 깨우는 것은 바로 끝나고, 켜졌는지는 목록이 알려준다. 실행과 달리 진행 중 상태를 들지 않는다.
    fun onWake(device: EmulatorDevice) {
        viewModelScope.launch { wakeDevice(device) }
    }

    private companion object {
        // 콜드 부트가 1분을 넘기기도 한다. 이보다 오래 걸리면 뜨지 않은 것으로 보고 버튼을 되살린다.
        // 정상적으로 뜬 기기의 잠금은 이 시간과 무관하게 목록이 푼다.
        val LaunchTimeout = 2.minutes
    }
}
