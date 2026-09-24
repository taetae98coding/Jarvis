package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.LaunchEmulatorUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorScreenUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import io.github.taetae98coding.jarvis.domain.emulator.SendEmulatorGestureUseCase
import io.github.taetae98coding.jarvis.domain.emulator.WakeDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.domain.rotation.ObserveDeviceRotationStatusUseCase
import io.github.taetae98coding.jarvis.domain.rotation.RotateDeviceUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationAngleUseCase
import io.github.taetae98coding.jarvis.domain.rotation.SetDeviceRotationLockUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.terminal.IsClaudeSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsTerminalSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.OpenTerminalSessionUseCase
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Koin 은 런타임에 해석한다. 정의를 빼먹어도 컴파일은 통과하고 앱이 뜰 때 죽으므로, 그 간격을
 * 이 테스트가 메운다. 네 타깃이 같은 모듈 선언을 쓰기 때문에 JVM 하나에서 확인한다.
 *
 * ViewModel 정의는 여기서 보지 않는다. `ViewModelStoreOwner` 없이 꺼낼 수 없어서 `:ui` 의 화면
 * 테스트가 대신 확인한다.
 */
class JarvisKoinTest {
    @AfterTest
    fun tearDown() {
        // startIsIdempotent 가 전역 Koin 을 쓴다. 다른 테스트에 남기지 않는다.
        if (KoinPlatformTools.defaultContext().getOrNull() != null) {
            stopKoin()
        }
    }

    @Test
    fun everyDefinitionResolves() {
        val koin = jarvisKoin()

        assertNotNull(koin.get<AppInfoRepository>())
        assertNotNull(koin.get<EmulatorRepository>())
        assertNotNull(koin.get<ScreenAwakeSettingsRepository>())
        assertNotNull(koin.get<ScreenAwakeRepository>())
        assertNotNull(koin.get<SystemScreenAwakeRepository>())
        assertNotNull(koin.get<DeviceRotationRepository>())
        assertNotNull(koin.get<TerminalRepository>())

        assertNotNull(koin.get<GetAppInfoUseCase>())
        assertNotNull(koin.get<ObserveEmulatorStatusUseCase>())
        assertNotNull(koin.get<ObserveEmulatorDevicesUseCase>())
        assertNotNull(koin.get<ObserveEmulatorScreenUseCase>())
        assertNotNull(koin.get<SendEmulatorGestureUseCase>())
        assertNotNull(koin.get<LaunchEmulatorUseCase>())
        assertNotNull(koin.get<WakeDeviceUseCase>())
        assertNotNull(koin.get<ObserveKeepScreenAwakeUseCase>())
        assertNotNull(koin.get<ObserveKeepSystemScreenAwakeUseCase>())
        assertNotNull(koin.get<ObserveSystemScreenAwakeStatusUseCase>())
        assertNotNull(koin.get<SetKeepScreenAwakeUseCase>())
        assertNotNull(koin.get<SetKeepSystemScreenAwakeUseCase>())
        assertNotNull(koin.get<ApplyKeepScreenAwakeUseCase>())
        assertNotNull(koin.get<ApplySystemScreenAwakeUseCase>())
        assertNotNull(koin.get<ObserveDeviceRotationStatusUseCase>())
        assertNotNull(koin.get<SetDeviceRotationAngleUseCase>())
        assertNotNull(koin.get<SetDeviceRotationLockUseCase>())
        assertNotNull(koin.get<RotateDeviceUseCase>())
        assertNotNull(koin.get<IsTerminalSupportedUseCase>())
        assertNotNull(koin.get<IsClaudeSupportedUseCase>())
        assertNotNull(koin.get<OpenTerminalSessionUseCase>())

        assertNotNull(koin.get<PlatformContext>())
        assertNotNull(koin.get<CoroutineScope>())
    }

    // 리포지토리가 둘이 되면 폴링이 두 번 돌고 설정 변경이 서로에게 보이지 않는다.
    @Test
    fun repositoriesAreSingletons() {
        val koin = jarvisKoin()

        assertSame(koin.get<ScreenAwakeSettingsRepository>(), koin.get<ScreenAwakeSettingsRepository>())
        assertSame(koin.get<EmulatorRepository>(), koin.get<EmulatorRepository>())
        assertSame(koin.get<DeviceRotationRepository>(), koin.get<DeviceRotationRepository>())
    }

    // Android 의 onCreate 와 iOS 의 MainViewController() 는 두 번 돌 수 있다.
    @Test
    fun startIsIdempotent() {
        startJarvisKoinWith(PlatformContext())
        val first = KoinPlatformTools.defaultContext().get()

        startJarvisKoinWith(PlatformContext())

        assertSame(first, KoinPlatformTools.defaultContext().get())
    }

    private fun jarvisKoin(): Koin = koinApplication { modules(jarvisModules(PlatformContext())) }.koin
}
