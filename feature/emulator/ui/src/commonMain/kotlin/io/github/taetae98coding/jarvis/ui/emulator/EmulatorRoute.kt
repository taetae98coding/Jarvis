package io.github.taetae98coding.jarvis.ui.emulator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 이 기능이 갖는 화면 셋. 백스택에 저장·복원되므로 직렬화 가능한 값만 담는다.
 *
 * 앱 셸은 이 타입을 모른다. 등록은 [emulatorUiModule] 이 한다.
 */
@Serializable
internal sealed interface EmulatorRoute : NavKey {
    @Serializable
    data object Devices : EmulatorRoute

    /**
     * 기기 상태가 아니라 식별자만 나른다. 이름·권한 같은 값은 화면이 열려 있는 동안에도 바뀌므로
     * 복사해 온 값보다 목록이 사실이다.
     */
    @Serializable
    data class Screen(val deviceId: String) : EmulatorRoute

    @Serializable
    data object Pairing : EmulatorRoute
}
