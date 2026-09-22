package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Stable
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.LaunchEmulatorUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * 앱 범위 화면 상태. [JarvisApp] 보다 위에서 한 번 만들어지므로 화면을 옮겨 다녀도 값이 유지된다.
 *
 * 유스케이스만 받는다. 저장소도 플랫폼 API 도 모르고, 테스트는 가짜 리포지토리로 만든 유스케이스를
 * 끼운다.
 */
@Stable
class JarvisAppState(
    private val scope: CoroutineScope,
    getAppInfo: GetAppInfoUseCase,
    observeEmulatorStatus: ObserveEmulatorStatusUseCase,
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
    private val observeEmulatorScreen: ObserveEmulatorScreenUseCase,
    private val sendEmulatorGesture: SendEmulatorGestureUseCase,
    private val launchEmulator: LaunchEmulatorUseCase,
    private val wakeDevice: WakeDeviceUseCase,
    observeKeepScreenAwake: ObserveKeepScreenAwakeUseCase,
    observeKeepSystemScreenAwake: ObserveKeepSystemScreenAwakeUseCase,
    observeSystemScreenAwakeStatus: ObserveSystemScreenAwakeStatusUseCase,
    observeDeviceRotationStatus: ObserveDeviceRotationStatusUseCase,
    private val setKeepScreenAwake: SetKeepScreenAwakeUseCase,
    private val setKeepSystemScreenAwake: SetKeepSystemScreenAwakeUseCase,
    private val applyKeepScreenAwake: ApplyKeepScreenAwakeUseCase,
    private val applySystemScreenAwake: ApplySystemScreenAwakeUseCase,
    private val setDeviceRotationAngle: SetDeviceRotationAngleUseCase,
    private val setDeviceRotationLock: SetDeviceRotationLockUseCase,
    private val rotateDevice: RotateDeviceUseCase,
) {
    val appInfo: AppInfo = getAppInfo()

    // 아직 답하지 않은 상태가 null 이다. 빈 상태로 시작하면 세는 중인데도 "0개" 를 사실인 것처럼
    // 보여주게 된다.
    val emulatorStatus: StateFlow<EmulatorStatus?> =
        observeEmulatorStatus().stateIn(scope, SharingStarted.Eagerly, null)

    // 목록은 화면이 열려 있는 동안에만 센다. 개수와 달리 실행 중인 에뮬레이터 수만큼 명령이 더
    // 도는 조회라, 아무도 보지 않을 때까지 5초마다 돌릴 이유가 없다.
    val emulatorDevices: StateFlow<List<EmulatorDevice>> =
        observeEmulatorDevices().stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val keepScreenAwake: StateFlow<Boolean> = observeKeepScreenAwake()

    val keepSystemScreenAwake: StateFlow<Boolean> = observeKeepSystemScreenAwake()

    val systemScreenAwake: StateFlow<SystemScreenAwakeStatus> = observeSystemScreenAwakeStatus()

    private val route = MutableStateFlow<EmulatorRoute?>(null)

    internal val emulatorRoute: StateFlow<EmulatorRoute?> = route.asStateFlow()

    internal fun onEmulatorCardClick() {
        route.value = EmulatorRoute.Devices
    }

    // 실행을 누른 기기. 여기 남아 있다고 해서 아직 뜨지 않았다는 뜻은 아니다.
    private val launchRequests = MutableStateFlow(emptySet<String>())

    /**
     * 실행을 눌렀고 아직 뜨지 않은 기기. 뜨는 데 수십 초가 걸리는 동안 버튼을 잠가 둔다.
     *
     * 값을 목록에서 유도한다. 켜졌다는 신호를 코루틴이 받아서 집합을 고치게 두면, 그 코루틴이
     * 전이를 한 번 놓쳤을 때 버튼이 [LaunchTimeout] 동안 잠긴 채로 남는다. 목록이 곧 사실이므로
     * 목록에서 그 기기가 꺼진 채로 보이지 않으면 잠금도 풀린다.
     */
    internal val launchingDevices: StateFlow<Set<String>> =
        combine(launchRequests, emulatorDevices) { requested, devices ->
            requested.filterTo(mutableSetOf()) { id -> devices.any { it.id == id && !it.isRunning } }
        }.stateIn(scope, SharingStarted.WhileSubscribed(), emptySet())

    internal fun onEmulatorDeviceClick(device: EmulatorDevice) {
        if (!device.canStream) return

        route.value = EmulatorRoute.Screen(device)
    }

    internal fun onEmulatorDeviceLaunch(device: EmulatorDevice) {
        if (!device.canLaunch || device.id in launchRequests.value) return

        launchRequests.value += device.id

        scope.launch {
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

    internal fun onEmulatorBack() {
        route.value = if (route.value is EmulatorRoute.Screen) EmulatorRoute.Devices else null
    }

    /** 수집하는 동안에만 기기 화면을 찍는다. 화면을 벗어나면 촬영도 멈춘다. */
    internal fun emulatorScreen(deviceId: String): Flow<ByteArray?> = observeEmulatorScreen(deviceId)

    // 깨우는 것은 바로 끝나고, 켜졌는지는 목록이 알려준다. 실행과 달리 진행 중 상태를 들지 않는다.
    internal fun onEmulatorDeviceWake(device: EmulatorDevice) {
        scope.launch { wakeDevice(device) }
    }

    internal fun onEmulatorGesture(device: EmulatorDevice, gesture: EmulatorGesture) {
        scope.launch { sendEmulatorGesture(device, gesture) }
    }

    val deviceRotation: StateFlow<DeviceRotationStatus> = observeDeviceRotationStatus()

    fun onKeepScreenAwakeChange(value: Boolean) {
        setKeepScreenAwake(value)
    }

    fun onKeepSystemScreenAwakeChange(value: Boolean) {
        setKeepSystemScreenAwake(value)
    }

    fun onDeviceRotationAngleClick(angle: RotationAngle) {
        setDeviceRotationAngle(angle)
    }

    fun onDeviceRotate(steps: Int) {
        rotateDevice(steps)
    }

    fun onDeviceRotationLockChange(locked: Boolean) {
        setDeviceRotationLock(locked)
    }

    /**
     * 설정을 따라 플랫폼 효과를 걸어 둔다. 취소될 때까지 돌아가므로 화면이 살아 있는 동안 한 번만
     * 부른다. 취소될 때 무엇을 되돌리는지는 각 유스케이스가 정한다.
     */
    internal suspend fun applyEffects() {
        coroutineScope {
            launch { applyKeepScreenAwake() }
            launch { applySystemScreenAwake() }
        }
    }

    private companion object {
        // 콜드 부트가 1분을 넘기기도 한다. 이보다 오래 걸리면 뜨지 않은 것으로 보고 버튼을 되살린다.
        // 정상적으로 뜬 기기의 잠금은 이 시간과 무관하게 목록이 푼다.
        val LaunchTimeout = 2.minutes
    }
}
