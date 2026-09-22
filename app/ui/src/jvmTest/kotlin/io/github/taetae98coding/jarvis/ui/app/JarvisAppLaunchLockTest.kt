package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorLaunchTestTag
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 실행 잠금이 화면에 그려지는 것까지 보는 테스트. 다른 화면 테스트와 달리 `jvmTest` 에만 있다.
 *
 * 잠금 값은 `EmulatorDevicesViewModel` 의 `combine` → `stateIn` 을 지나 화면에 온다. 그 전파는
 * `viewModelScope`(`Dispatchers.Main`)에서 일어나고, Wasm 에서는 `waitUntil` 의 폴링 루프가 같은
 * 이벤트 루프를 잡고 있어서 10초 안에 도착하지 못한다. 잠금 규칙 자체는 화면을 그리지 않는
 * `EmulatorDevicesViewModelTest` 가 본다.
 */
@OptIn(ExperimentalTestApi::class)
class JarvisAppLaunchLockTest {
    // 뜨는 데 수십 초가 걸린다. 그동안 버튼이 살아 있으면 같은 AVD 에 요청이 여러 번 나간다.
    @Test
    fun launchingDeviceLocksTheButton() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))
        setContent { TestJarvisApp(emulator = emulator) }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorLaunchTestTag(StoppedAndroidDevice.id)).performClick()

        // 상태가 화면에 반영되는 것까지 기다린다. 리포지토리에 요청이 닿은 시점과 다시 그려지는
        // 시점이 달라서, 기록만 보고 단언하면 어쩌다 한 번 앞질러 읽는다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithText("켜는 중…").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
        onNodeWithTag(emulatorLaunchTestTag(StoppedAndroidDevice.id)).assertIsNotEnabled()
    }

    private companion object {
        // 첫 전파는 Flow 를 한 바퀴 돌아야 한다.
        const val FrameTimeoutMillis = 10_000L
    }
}
