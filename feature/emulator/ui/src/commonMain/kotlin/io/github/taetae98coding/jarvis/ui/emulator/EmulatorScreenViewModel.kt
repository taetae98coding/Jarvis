package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 기기 화면 하나의 상태. 라우트가 [deviceId] 만 나르므로 기기의 나머지 값은 목록에서 찾는다.
 */
internal class EmulatorScreenViewModel(
    val deviceId: String,
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
    observeEmulatorScreen: ObserveEmulatorScreenUseCase,
    private val sendEmulatorGesture: SendEmulatorGestureUseCase,
) : ViewModel() {
    // 목록이 첫 답을 하기 전에는 null 이다. 그동안 화면은 제목 없이 "가져오는 중" 만 보여준다.
    val device: StateFlow<EmulatorDevice?> =
        observeEmulatorDevices()
            .map { devices -> devices.firstOrNull { it.id == deviceId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /** 수집하는 동안에만 기기 화면을 찍는다. 화면을 벗어나면 촬영도 멈춘다. */
    val frames: Flow<ByteArray?> = observeEmulatorScreen(deviceId)

    fun onGesture(gesture: EmulatorGesture) {
        val target = device.value ?: return

        viewModelScope.launch { sendEmulatorGesture(target, gesture) }
    }
}
