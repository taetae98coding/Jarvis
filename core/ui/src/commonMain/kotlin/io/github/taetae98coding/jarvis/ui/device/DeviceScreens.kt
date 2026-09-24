package io.github.taetae98coding.jarvis.ui.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/** 새 탭 메뉴에 나오는 기기 한 대. [kind] 는 그대로 보여 줄 종류 문구다("Android 에뮬레이터" 등). */
@Immutable
data class DeviceChoice(
    val id: String,
    val name: String,
    val kind: String,
)

/**
 * 터미널이 기기 화면을 탭에 그리는 이음새. 구현은 emulator 기능이 Koin 에 등록하고, 터미널은 이 인터페이스만
 * 본다 — 기능은 서로를 의존하지 않는다(docs/common/terminal-device.html#implementation).
 */
@Stable
interface DeviceScreens {
    /** 지금 화면을 볼 수 있는 기기. null 은 아직 모른다는 뜻이다. 컴포지션에 있는 동안에만 센다. */
    @Composable
    fun choices(): List<DeviceChoice>?

    /** [deviceId] 기기의 화면. 탭·드래그가 기기로 간다. 컴포지션에 있는 동안에만 찍는다. */
    @Composable
    fun Screen(deviceId: String, modifier: Modifier)
}
