package io.github.taetae98coding.jarvis.shared

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
import io.github.taetae98coding.jarvis.shared.platform.EmulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.EmulatorStatus
import io.github.taetae98coding.jarvis.shared.platform.EmulatorSummary
import io.github.taetae98coding.jarvis.shared.platform.InMemorySettingsStore
import io.github.taetae98coding.jarvis.shared.platform.fakeEmulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.platformName
import io.github.taetae98coding.jarvis.shared.settings.AppSettings
import io.github.taetae98coding.jarvis.shared.ui.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.shared.ui.KeepSystemScreenAwakeTestTag
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @Test
    fun showsAppVersion() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onNodeWithText(APP_VERSION).assertIsDisplayed()
    }

    @Test
    fun showsCurrentPlatformName() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onNodeWithText(platformName).assertIsDisplayed()
    }

    @Test
    fun keepScreenAwakeDefaultsToOff() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOff()
    }

    @Test
    fun keepScreenAwakeTogglesOn() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick().assertIsOn()
    }

    @Test
    fun keepScreenAwakeIsPersisted() = runComposeUiTest {
        val store = InMemorySettingsStore()
        setContent { App(store, fakeEmulatorProbe()) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick()

        assertTrue(store.getBoolean(AppSettings.KeepScreenAwakeKey, false))
    }

    @Test
    fun keepScreenAwakeIsRestoredOnRelaunch() = runComposeUiTest {
        val store = InMemorySettingsStore()
        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)

        setContent { App(store, fakeEmulatorProbe()) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }

    @Test
    fun keepScreenAwakeFollowsChangesMadeOutsideTheApp() = runComposeUiTest {
        val store = InMemorySettingsStore()
        setContent { App(store, fakeEmulatorProbe()) }

        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }

    // 전역 화면 유지는 Android 만 지원한다. Skiko 로 렌더링하는 타깃에서는 토글이 잠겨 있어야 한다.
    @Test
    fun systemScreenAwakeIsLockedWhereItIsNotSupported() = runComposeUiTest {
        val store = InMemorySettingsStore()
        setContent { App(store, fakeEmulatorProbe()) }

        onNodeWithTag(KeepSystemScreenAwakeTestTag)
            .assertIsNotEnabled()
            .performClick()
            .assertIsOff()

        assertFalse(store.getBoolean(AppSettings.KeepSystemScreenAwakeKey, false))
    }

    @Test
    fun systemScreenAwakeExplainsWhyItIsUnavailable() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onNodeWithText("이 플랫폼에서는 앱이 없는 동안의 화면 꺼짐을 막을 수 없습니다.").assertIsDisplayed()
    }

    @Test
    fun showsEmulatorCounts() = runComposeUiTest {
        setContent {
            App(
                store = InMemorySettingsStore(),
                probe = fakeEmulatorProbe(
                    android = EmulatorSummary(total = 5, running = 2),
                    ios = EmulatorSummary(total = 11, running = 0),
                ),
            )
        }

        onNodeWithText("실행 중 2개 / 전체 5개").assertIsDisplayed()
        onNodeWithText("실행 중 0개 / 전체 11개").assertIsDisplayed()
    }

    @Test
    fun tellsZeroApartFromUncountable() = runComposeUiTest {
        setContent {
            App(
                store = InMemorySettingsStore(),
                probe = fakeEmulatorProbe(android = EmulatorSummary(total = 0, running = 0), ios = null),
            )
        }

        onNodeWithText("실행 중 0개 / 전체 0개").assertIsDisplayed()
        onNodeWithText("셀 수 없음").assertIsDisplayed()
    }

    @Test
    fun showsUncountableWhenNothingCanCount() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onAllNodesWithText("셀 수 없음").assertCountEquals(2)
    }

    @Test
    fun showsPlaceholderUntilTheProbeAnswers() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), EmulatorProbe { emptyFlow() }) }

        onAllNodesWithText("확인 중…").assertCountEquals(2)
    }

    @Test
    fun emulatorCountsFollowProbeUpdates() = runComposeUiTest {
        val probe = fakeEmulatorProbe(android = EmulatorSummary(total = 3, running = 0))
        setContent { App(InMemorySettingsStore(), probe) }
        onNodeWithText("실행 중 0개 / 전체 3개").assertIsDisplayed()

        probe.status.value = EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))

        onNodeWithText("실행 중 1개 / 전체 3개").assertIsDisplayed()
    }
}
