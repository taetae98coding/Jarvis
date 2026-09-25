package io.github.taetae98coding.jarvis.ui.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * 새 탭 메뉴에 나오는 기기 한 대. [kind] 는 그대로 보여 줄 종류 문구다("Android 에뮬레이터" 등). [platform] 은 터미널이
 * 탭의 종류 아이콘을 고르려고 탭에 적어 둔다.
 *
 * [isRunning]·[isPhysical]·[canMirror] 는 실행 메뉴가 쓴다(docs/common/terminal-run.html). [canMirror] 는 켜지면 화면을 볼 수
 * 있는 기기(Android 전부, iOS 시뮬레이터)다. [DeviceScreens.choices] 의 기기는 모두 켜져 있고 화면을 볼 수 있다.
 */
@Immutable
data class DeviceChoice(
    val id: String,
    val name: String,
    val kind: String,
    val platform: DeviceChoicePlatform,
    val isRunning: Boolean = true,
    val isPhysical: Boolean = false,
    val canMirror: Boolean = true,
)

enum class DeviceChoicePlatform {
    Android,
    IOS,
}

/**
 * 터미널이 기기 화면을 탭에 그리는 이음새. 구현은 emulator 기능이 Koin 에 등록하고, 터미널은 이 인터페이스만
 * 본다 — 기능은 서로를 의존하지 않는다(docs/common/terminal-device.html#implementation).
 */
@Stable
interface DeviceScreens {
    /** 지금 화면을 볼 수 있는 기기. null 은 아직 모른다는 뜻이다. 컴포지션에 있는 동안에만 센다. */
    @Composable
    fun choices(): List<DeviceChoice>?

    /** 앱을 실행할 수 있는 기기 전부. 꺼진 가상 기기와 화면을 볼 수 없는 실물 iOS 도 든다. null 은 아직 모른다는 뜻이다. */
    @Composable
    fun runTargets(): List<DeviceChoice>?

    /** [deviceId] 기기의 화면. 탭·드래그가 기기로 간다. 컴포지션에 있는 동안에만 찍는다. */
    @Composable
    fun Screen(deviceId: String, modifier: Modifier)

    /** [deviceId] 기기의 로그 창(필터·지우기 포함). 컴포지션에 있는 동안에만 읽는다(docs/common/device-logcat.html). */
    @Composable
    fun Log(deviceId: String, modifier: Modifier)
}
