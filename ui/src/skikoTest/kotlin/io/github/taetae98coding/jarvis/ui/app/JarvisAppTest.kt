package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorFrameTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorListTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorScreenTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorDeviceTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorLaunchTestTag
import io.github.taetae98coding.jarvis.ui.emulator.emulatorWakeTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationBackwardTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationForwardTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationLockTestTag
import io.github.taetae98coding.jarvis.ui.rotation.deviceRotationAngleTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeTestTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JarvisAppTest {
    @Test
    fun showsAppVersion() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState()) }

        onNodeWithText(TestAppInfo.version).assertIsDisplayed()
    }

    @Test
    fun showsCurrentPlatformName() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState()) }

        onNodeWithText(TestAppInfo.platform).assertIsDisplayed()
    }

    @Test
    fun keepScreenAwakeDefaultsToOff() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState()) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOff()
    }

    @Test
    fun keepScreenAwakeTogglesOn() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState()) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick().assertIsOn()
    }

    @Test
    fun keepScreenAwakeIsPersisted() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { JarvisApp(rememberTestJarvisAppState(settings = settings)) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick()

        assertTrue(settings.keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeIsRestoredOnRelaunch() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository(keepScreenAwake = true)

        setContent { JarvisApp(rememberTestJarvisAppState(settings = settings)) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }

    @Test
    fun keepScreenAwakeFollowsChangesMadeOutsideTheApp() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { JarvisApp(rememberTestJarvisAppState(settings = settings)) }

        settings.setKeepScreenAwake(true)

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }

    // 전역 화면 유지는 Android 만 지원한다. Skiko 로 렌더링하는 타깃에서는 토글이 잠겨 있어야 한다.
    @Test
    fun systemScreenAwakeIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        setContent { JarvisApp(rememberTestJarvisAppState(settings = settings)) }

        onNodeWithTag(KeepSystemScreenAwakeTestTag)
            .assertIsNotEnabled()
            .performClick()
            .assertIsOff()

        assertFalse(settings.keepSystemScreenAwake.value)
    }

    @Test
    fun systemScreenAwakeExplainsWhyItIsUnavailable() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState()) }

        onNodeWithText("이 플랫폼에서는 앱이 없는 동안의 화면 꺼짐을 막을 수 없습니다.").assertIsDisplayed()
    }

    // 지원하는 플랫폼(Android)의 화면은 가짜 상태로만 확인할 수 있다. Android 에는 UI 테스트가 없다.
    @Test
    fun systemScreenAwakeTogglesWhereItIsSupported() = runComposeUiTest {
        val settings = FakeScreenAwakeSettingsRepository()
        val system = FakeSystemScreenAwakeRepository(
            SystemScreenAwakeStatus(supported = true, permitted = true),
        )
        setContent {
            JarvisApp(rememberTestJarvisAppState(settings = settings, systemScreenAwake = system))
        }

        onNodeWithTag(KeepSystemScreenAwakeTestTag).performClick().assertIsOn()

        assertTrue(settings.keepSystemScreenAwake.value)
    }

    @Test
    fun showsEmulatorCounts() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(
                        android = EmulatorSummary(total = 5, running = 2),
                        ios = EmulatorSummary(total = 11, running = 0),
                    ),
                ),
            )
        }

        onNodeWithText("실행 중 2개 / 전체 5개").assertIsDisplayed()
        onNodeWithText("실행 중 0개 / 전체 11개").assertIsDisplayed()
    }

    @Test
    fun tellsZeroApartFromUncountable() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(android = EmulatorSummary(), ios = null),
                ),
            )
        }

        onNodeWithText("실행 중 0개 / 전체 0개").assertIsDisplayed()
        onNodeWithText("셀 수 없음").assertIsDisplayed()
    }

    @Test
    fun showsUncountableWhenNothingCanCount() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState()) }

        onAllNodesWithText("셀 수 없음", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun showsPlaceholderUntilTheRepositoryAnswers() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = SilentEmulatorRepository)) }

        onAllNodesWithText("확인 중…", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun emulatorCountsFollowRepositoryUpdates() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(android = EmulatorSummary(total = 3, running = 0))
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        onNodeWithText("실행 중 0개 / 전체 3개").assertIsDisplayed()

        emulator.status.value = EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))

        onNodeWithText("실행 중 1개 / 전체 3개").assertIsDisplayed()
    }

    @Test
    fun emulatorCardOpensDeviceList() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
                ),
            )
        }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()
    }

    @Test
    fun deviceListShowsNameAndState() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(
                        devices = listOf(RunningAndroidDevice, StoppedAndroidDevice, RunningSimulator),
                    ),
                ),
            )
        }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText(RunningAndroidDevice.name).assertIsDisplayed()
        onNodeWithText("Android · 실행 중").assertIsDisplayed()
        onNodeWithText("Android · 꺼짐").assertIsDisplayed()
        onNodeWithText("iOS · 실행 중").assertIsDisplayed()
    }

    // 꺼져 있는 기기에는 찍을 화면이 없다. 눌러도 되는 것처럼 보이면 빈 화면만 보게 된다.
    @Test
    fun stoppedDeviceCannotBeOpened() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(
                        devices = listOf(RunningAndroidDevice, StoppedAndroidDevice),
                    ),
                ),
            )
        }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorDeviceTestTag(StoppedAndroidDevice.id))
            .assertIsNotEnabled()
            .performClick()

        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()
        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).assertIsEnabled()
    }

    @Test
    fun emptyDeviceListExplainsWhy() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = FakeEmulatorRepository())) }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText("연결된 기기가 없거나 개발자 머신에 물어볼 수 없습니다.").assertIsDisplayed()
    }

    @Test
    fun physicalDeviceIsMarkedInTheList() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(PhysicalAndroidDevice))
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText(PhysicalAndroidDevice.name).assertIsDisplayed()
        onNodeWithText("Android · 실물 기기 · 연결됨").assertIsDisplayed()
        onNodeWithTag(emulatorDeviceTestTag(PhysicalAndroidDevice.id)).assertIsEnabled()
    }

    // 연결됐는데 목록에 없으면 "꽂았는데 왜 없지" 가 된다. 대신 왜 누를 수 없는지 그 줄에 적는다.
    @Test
    fun deviceWithoutAScreenSaysSoInTheList() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(PhysicalIosDevice))
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }

        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText("iOS · 실물 기기 · 연결됨 · 화면을 볼 수 없음").assertIsDisplayed()
        onNodeWithTag(emulatorDeviceTestTag(PhysicalIosDevice.id)).assertIsNotEnabled()
    }

    // 화면이 꺼진 기기는 스트리밍을 열어도 검은 화면만 보인다. 목록에서 켤 수 있어야 한다.
    @Test
    fun sleepingDeviceCanBeWokenUp() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(SleepingAndroidDevice, PhysicalAndroidDevice, StoppedAndroidDevice),
        )
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithText("Android · 실물 기기 · 연결됨 · 화면 꺼짐").assertIsDisplayed()
        onNodeWithTag(emulatorWakeTestTag(SleepingAndroidDevice.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.woken.isNotEmpty() }
        assertEquals(listOf(SleepingAndroidDevice.id), emulator.woken.toList())
        // 깨어 있는 기기와 입력을 받지 못하는 꺼진 AVD 에는 붙지 않는다.
        onAllNodesWithTag(emulatorWakeTestTag(PhysicalAndroidDevice.id)).assertCountEquals(0)
        onAllNodesWithTag(emulatorWakeTestTag(StoppedAndroidDevice.id)).assertCountEquals(0)
    }

    @Test
    fun stoppedDeviceCanBeLaunched() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(RunningAndroidDevice, StoppedAndroidDevice),
        )
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorLaunchTestTag(StoppedAndroidDevice.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.launched.isNotEmpty() }
        assertEquals(listOf(StoppedAndroidDevice.id), emulator.launched.toList())
        // 이미 켜져 있는 기기에는 켤 것이 없다.
        onAllNodesWithTag(emulatorLaunchTestTag(RunningAndroidDevice.id)).assertCountEquals(0)
    }

    // 뜨는 데 수십 초가 걸린다. 그동안 버튼이 살아 있으면 같은 AVD 에 요청이 여러 번 나간다.
    // 기기가 뜬 뒤 잠금이 풀리는 것은 JarvisAppStateTest 가 본다. 목록이 바뀌는 것을 화면에서
    // 기다리면 Wasm 에서 폴링 루프가 이벤트 루프를 잡아 전파가 밀린다.
    @Test
    fun launchingDeviceLocksTheButton() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(devices = listOf(StoppedAndroidDevice))
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
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

    @Test
    fun runningDeviceOpensStreamScreen() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
                ),
            )
        }
        onNodeWithTag(EmulatorTestTag).performClick()

        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).performClick()

        onNodeWithTag(EmulatorScreenTestTag).assertIsDisplayed()
        // 아직 프레임이 오지 않은 상태와 가져오지 못한 상태는 다르다.
        onNodeWithText("화면을 가져오는 중…").assertIsDisplayed()
    }

    @Test
    fun tapOnStreamIsSentInDeviceCoordinates() = runComposeUiTest {
        val emulator = streamingEmulator()
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        openStream()

        onNodeWithTag(EmulatorFrameTestTag).performTouchInput { click(center) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.gestures.isNotEmpty() }
        assertEquals(
            listOf(
                RunningAndroidDevice.id to EmulatorGesture.Tap(
                    x = TestFrameWidth / 2,
                    y = TestFrameHeight / 2,
                ),
            ),
            emulator.gestures.toList(),
        )
    }

    @Test
    fun dragOnStreamIsSentAsSwipe() = runComposeUiTest {
        val emulator = streamingEmulator()
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        openStream()

        onNodeWithTag(EmulatorFrameTestTag).performTouchInput { swipe(start = centerLeft, end = centerRight) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.gestures.isNotEmpty() }
        val swipe = assertIs<EmulatorGesture.Swipe>(emulator.gestures.single().second)
        assertEquals(0, swipe.fromX)
        assertEquals(TestFrameWidth - 1, swipe.toX)
        assertEquals(TestFrameHeight / 2, swipe.fromY)
        assertEquals(TestFrameHeight / 2, swipe.toY)
        // 0ms 스와이프는 기기가 플릭으로 받아 화면이 튕긴다.
        assertTrue(swipe.durationMillis >= 50)
    }

    // iOS 시뮬레이터에는 입력을 주입하는 도구가 없다. 화면은 보이되 왜 안 되는지 말해야 한다.
    @Test
    fun uncontrollableDeviceSaysSo() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(
            devices = listOf(RunningSimulator),
            frames = MutableStateFlow(TestFrame),
        )
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(RunningSimulator.id)).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(EmulatorFrameTestTag).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(EmulatorFrameTestTag).performTouchInput { click(center) }

        onNodeWithText("이 기기에는 제스처를 보낼 수 없습니다. 화면만 볼 수 있습니다.").assertIsDisplayed()
        assertTrue(emulator.gestures.isEmpty())
    }

    @Test
    fun backReturnsToListThenGrid() = runComposeUiTest {
        setContent {
            JarvisApp(
                rememberTestJarvisAppState(
                    emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice)),
                ),
            )
        }
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).performClick()

        onNodeWithText("← 뒤로").performClick()
        onNodeWithTag(EmulatorListTestTag).assertIsDisplayed()

        onNodeWithText("← 뒤로").performClick()
        onNodeWithTag(EmulatorTestTag).assertIsDisplayed()
    }

    private fun streamingEmulator() = FakeEmulatorRepository(
        devices = listOf(RunningAndroidDevice),
        frames = MutableStateFlow(TestFrame),
    )

    private fun ComposeUiTest.openStream() {
        onNodeWithTag(EmulatorTestTag).performClick()
        onNodeWithTag(emulatorDeviceTestTag(RunningAndroidDevice.id)).performClick()

        // 프레임이 그려지기 전에 누르면 좌표를 맞출 기준이 없다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(EmulatorFrameTestTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        // 첫 프레임은 Flow 를 한 바퀴 돌고 디코딩까지 끝나야 그려진다. 브라우저에서는 그게 기본
        // 1초를 넘길 때가 있다.
        const val FrameTimeoutMillis = 10_000L
    }

    // 화면 회전을 지원하는 것은 Android · iOS · Web 이고, 그 셋의 화면은 가짜 상태로만 확인할 수 있다.
    private fun rotatable(angle: RotationAngle = RotationAngle.Degrees0) =
        FakeDeviceRotationRepository(DeviceRotationStatus(supported = true, angle = angle))

    @Test
    fun deviceRotationShowsCurrentAngle() = runComposeUiTest {
        val rotation = rotatable(RotationAngle.Degrees90)
        setContent { JarvisApp(rememberTestJarvisAppState(deviceRotation = rotation)) }
        onNodeWithText("현재 90°, 센서를 따라 회전합니다.").assertIsDisplayed()

        rotation.status.value = rotation.status.value.copy(angle = RotationAngle.Degrees270)

        onNodeWithText("현재 270°, 센서를 따라 회전합니다.").assertIsDisplayed()
    }

    @Test
    fun deviceRotationAngleButtonLocksToThatAngle() = runComposeUiTest {
        val rotation = rotatable()
        setContent { JarvisApp(rememberTestJarvisAppState(deviceRotation = rotation)) }

        onNodeWithTag(deviceRotationAngleTestTag(RotationAngle.Degrees180)).performClick()

        assertEquals(RotationAngle.Degrees180, rotation.status.value.angle)
        assertTrue(rotation.status.value.locked)
    }

    @Test
    fun deviceRotationRotatesForwardAndBackward() = runComposeUiTest {
        val rotation = rotatable(RotationAngle.Degrees90)
        setContent { JarvisApp(rememberTestJarvisAppState(deviceRotation = rotation)) }

        onNodeWithTag(DeviceRotationForwardTestTag).performClick()
        assertEquals(RotationAngle.Degrees180, rotation.status.value.angle)

        onNodeWithTag(DeviceRotationBackwardTestTag).performClick()
        assertEquals(RotationAngle.Degrees90, rotation.status.value.angle)
    }

    @Test
    fun deviceRotationLockTogglesWhereItIsSupported() = runComposeUiTest {
        val rotation = rotatable()
        setContent { JarvisApp(rememberTestJarvisAppState(deviceRotation = rotation)) }

        onNodeWithTag(DeviceRotationLockTestTag).performClick().assertIsOn()

        assertTrue(rotation.status.value.locked)
    }

    // JVM 은 실제로 지원하지 않는 타깃이라, 여기서는 가짜가 아니라 실제 판정을 검증한다.
    @Test
    fun deviceRotationIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val rotation = FakeDeviceRotationRepository()
        setContent { JarvisApp(rememberTestJarvisAppState(deviceRotation = rotation)) }

        onNodeWithTag(DeviceRotationLockTestTag).assertIsNotEnabled().performClick().assertIsOff()
        onNodeWithTag(DeviceRotationForwardTestTag).assertIsNotEnabled().performClick()
        onNodeWithText("이 플랫폼에서는 화면을 돌릴 수 없습니다.").assertIsDisplayed()

        assertFalse(rotation.status.value.locked)
        assertNull(rotation.status.value.angle)
    }
}
