package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationBackwardTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationForwardTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationLockTestTag
import io.github.taetae98coding.jarvis.ui.rotation.deviceRotationAngleTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeTestTag
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

        onAllNodesWithText("셀 수 없음").assertCountEquals(2)
    }

    @Test
    fun showsPlaceholderUntilTheRepositoryAnswers() = runComposeUiTest {
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = EmulatorRepository { emptyFlow() })) }

        onAllNodesWithText("확인 중…").assertCountEquals(2)
    }

    @Test
    fun emulatorCountsFollowRepositoryUpdates() = runComposeUiTest {
        val emulator = FakeEmulatorRepository(android = EmulatorSummary(total = 3, running = 0))
        setContent { JarvisApp(rememberTestJarvisAppState(emulator = emulator)) }
        onNodeWithText("실행 중 0개 / 전체 3개").assertIsDisplayed()

        emulator.status.value = EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))

        onNodeWithText("실행 중 1개 / 전체 3개").assertIsDisplayed()
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
