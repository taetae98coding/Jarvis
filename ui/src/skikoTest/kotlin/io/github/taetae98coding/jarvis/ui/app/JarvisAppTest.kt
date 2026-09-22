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
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.ui.screen.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeTestTag
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertFalse
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
}
