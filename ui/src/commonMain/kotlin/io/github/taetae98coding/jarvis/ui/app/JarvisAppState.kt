package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Stable
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.GetAppInfoUseCase
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 범위 화면 상태. [JarvisApp] 보다 위에서 한 번 만들어지므로 화면을 옮겨 다녀도 값이 유지된다.
 *
 * 유스케이스만 받는다. 저장소도 플랫폼 API 도 모르고, 테스트는 가짜 리포지토리로 만든 유스케이스를
 * 끼운다.
 */
@Stable
class JarvisAppState(
    scope: CoroutineScope,
    getAppInfo: GetAppInfoUseCase,
    observeEmulatorStatus: ObserveEmulatorStatusUseCase,
    observeKeepScreenAwake: ObserveKeepScreenAwakeUseCase,
    observeKeepSystemScreenAwake: ObserveKeepSystemScreenAwakeUseCase,
    observeSystemScreenAwakeStatus: ObserveSystemScreenAwakeStatusUseCase,
    private val setKeepScreenAwake: SetKeepScreenAwakeUseCase,
    private val setKeepSystemScreenAwake: SetKeepSystemScreenAwakeUseCase,
    private val applyKeepScreenAwake: ApplyKeepScreenAwakeUseCase,
    private val applySystemScreenAwake: ApplySystemScreenAwakeUseCase,
) {
    val appInfo: AppInfo = getAppInfo()

    // 아직 답하지 않은 상태가 null 이다. 빈 상태로 시작하면 세는 중인데도 "0개" 를 사실인 것처럼
    // 보여주게 된다.
    val emulatorStatus: StateFlow<EmulatorStatus?> =
        observeEmulatorStatus().stateIn(scope, SharingStarted.Eagerly, null)

    val keepScreenAwake: StateFlow<Boolean> = observeKeepScreenAwake()

    val keepSystemScreenAwake: StateFlow<Boolean> = observeKeepSystemScreenAwake()

    val systemScreenAwake: StateFlow<SystemScreenAwakeStatus> = observeSystemScreenAwakeStatus()

    fun onKeepScreenAwakeChange(value: Boolean) {
        setKeepScreenAwake(value)
    }

    fun onKeepSystemScreenAwakeChange(value: Boolean) {
        setKeepSystemScreenAwake(value)
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
}
