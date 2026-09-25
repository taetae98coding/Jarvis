package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import io.github.taetae98coding.jarvis.domain.emulator.matches
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
            .map { devices -> devices.firstOrNull { it.id == deviceId } ?: devices.firstOrNull { it.matches(deviceId) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * 수집하는 동안에만 기기 화면을 찍는다. 화면을 벗어나면 촬영도 멈춘다. `avd:<이름>` 은 목록에서 켜진 에뮬레이터의 시리얼로
     * 풀릴 때마다 그 기기의 화면으로 갈아탄다(docs/common/terminal-run.html R11). 목록이 오기 전에는 받은 id 그대로 찍는다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val frames: Flow<EmulatorFrame?> = device
        .map { it?.id ?: deviceId }
        .distinctUntilChanged()
        .flatMapLatest { observeEmulatorScreen(it) }

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

    // 제스처를 바로 보내지 않고 큐에 넣어 순서대로 하나씩 보낸다. 제스처마다 launch 하면 로컬 에이전트를
    // 타는 타깃에서 UP 이 MOVE 를 앞질러, 뗀 뒤에도 끌린 것처럼 보인다.
    private val gestures = Channel<EmulatorGesture>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            var queued: EmulatorGesture? = gestures.receiveCatching().getOrNull()

            while (queued != null) {
                var current = queued
                queued = null

                // 보내는 동안 쌓인 MOVE·Hover 는 마지막만 남긴다. DOWN·UP·CANCEL 은 버리지 않고 다음에 보낸다.
                if (current.isCoalescible()) {
                    while (true) {
                        val next = gestures.tryReceive().getOrNull() ?: break
                        if (next.isCoalescible()) current = next else { queued = next; break }
                    }
                }

                device.value?.let { sendEmulatorGesture(it, current) }

                if (queued == null) queued = gestures.receiveCatching().getOrNull()
            }
        }
    }

    fun onGesture(gesture: EmulatorGesture) {
        gestures.trySend(gesture)
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

    private fun EmulatorGesture.isCoalescible(): Boolean =
        this is EmulatorGesture.Hover || (this is EmulatorGesture.Touch && action == TouchAction.MOVE)

    private companion object {
        // 목록 폴링(5초) 두 번. 그 안에 켜졌다는 답이 없으면 켜지지 않은 것으로 보고 버튼을 되살린다.
        val WakeTimeout = 10.seconds
    }
}
