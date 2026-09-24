package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * 기기 화면 하나의 상태. 라우트가 [deviceId] 만 나르므로 기기의 나머지 값은 목록에서 찾는다.
 */
internal class EmulatorScreenViewModel(
    val deviceId: String,
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
    observeEmulatorScreen: ObserveEmulatorScreenUseCase,
    private val sendEmulatorGesture: SendEmulatorGestureUseCase,
    private val wakeDevice: WakeDeviceUseCase,
) : ViewModel() {
    // 목록이 첫 답을 하기 전에는 null 이다. 그동안 화면은 제목 없이 "가져오는 중" 만 보여준다.
    val device: StateFlow<EmulatorDevice?> =
        observeEmulatorDevices()
            .map { devices -> devices.firstOrNull { it.id == deviceId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /** 수집하는 동안에만 기기 화면을 찍는다. 화면을 벗어나면 촬영도 멈춘다. */
    val frames: Flow<ByteArray?> = observeEmulatorScreen(deviceId)

    // 켜기를 눌렀다. 여기 남아 있다고 해서 아직 꺼져 있다는 뜻은 아니다.
    private val wakeRequested = MutableStateFlow(false)

    /**
     * 켜기를 눌렀고 목록이 아직 꺼져 있다고 하는 동안. 목록이 최대 5초 늦으므로 그동안 버튼을 다시
     * 내밀지 않는다. EmulatorDevicesViewModel.launchingDevices 와 같이 값을 목록에서 유도해서, 켜진
     * 것이 보이면 [WakeTimeout] 을 기다리지 않고 풀린다.
     */
    val isWaking: StateFlow<Boolean> =
        combine(wakeRequested, device) { requested, device -> requested && device?.isAsleep == true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun onGesture(gesture: EmulatorGesture) {
        val target = device.value ?: return

        viewModelScope.launch { sendEmulatorGesture(target, gesture) }
    }

    fun onWake() {
        val target = device.value ?: return
        if (wakeRequested.value) return

        wakeRequested.value = true

        viewModelScope.launch {
            try {
                wakeDevice(target)

                // 켜지지 않는 기기에서 버튼을 영영 숨기지 않기 위한 대비다.
                delay(WakeTimeout)
            } finally {
                wakeRequested.value = false
            }
        }
    }

    private companion object {
        // 목록 폴링(5초) 두 번. 그 안에 켜졌다는 답이 없으면 켜지지 않은 것으로 보고 버튼을 되살린다.
        val WakeTimeout = 10.seconds
    }
}
