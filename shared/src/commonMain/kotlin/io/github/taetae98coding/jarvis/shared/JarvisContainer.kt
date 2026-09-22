package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.DataModule
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.ui.app.JarvisAppState
import kotlinx.coroutines.CoroutineScope

/**
 * :data 의 리포지토리 → :domain 의 유스케이스 → :ui 의 화면 상태를 잇는다.
 *
 * 세 모듈이 서로를 모르므로 조립은 이 한 곳에서만 일어난다. 기능이 늘면 여기에 줄이 늘 뿐
 * 파일이 늘지는 않는다.
 */
internal class JarvisContainer(
    context: PlatformContext,
    scope: CoroutineScope,
) {
    private val data = DataModule(context, scope)

    private val setDeviceRotationAngle = SetDeviceRotationAngleUseCase(data.deviceRotationRepository)

    val appState = JarvisAppState(
        scope = scope,
        getAppInfo = GetAppInfoUseCase(data.appInfoRepository),
        observeEmulatorStatus = ObserveEmulatorStatusUseCase(data.emulatorRepository),
        observeKeepScreenAwake = ObserveKeepScreenAwakeUseCase(data.screenAwakeSettingsRepository),
        observeKeepSystemScreenAwake = ObserveKeepSystemScreenAwakeUseCase(data.screenAwakeSettingsRepository),
        observeSystemScreenAwakeStatus = ObserveSystemScreenAwakeStatusUseCase(data.systemScreenAwakeRepository),
        observeDeviceRotationStatus = ObserveDeviceRotationStatusUseCase(data.deviceRotationRepository),
        setKeepScreenAwake = SetKeepScreenAwakeUseCase(data.screenAwakeSettingsRepository),
        setKeepSystemScreenAwake = SetKeepSystemScreenAwakeUseCase(
            data.screenAwakeSettingsRepository,
            data.systemScreenAwakeRepository,
        ),
        applyKeepScreenAwake = ApplyKeepScreenAwakeUseCase(
            data.screenAwakeSettingsRepository,
            data.screenAwakeRepository,
        ),
        applySystemScreenAwake = ApplySystemScreenAwakeUseCase(
            data.screenAwakeSettingsRepository,
            data.systemScreenAwakeRepository,
        ),
        setDeviceRotationAngle = setDeviceRotationAngle,
        setDeviceRotationLock = SetDeviceRotationLockUseCase(data.deviceRotationRepository),
        rotateDevice = RotateDeviceUseCase(data.deviceRotationRepository, setDeviceRotationAngle),
    )
}
