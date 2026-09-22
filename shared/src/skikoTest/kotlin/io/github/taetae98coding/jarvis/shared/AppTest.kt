package io.github.taetae98coding.jarvis.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.shared.platform.EmulatorSummary
import io.github.taetae98coding.jarvis.shared.platform.InMemorySettingsStore
import io.github.taetae98coding.jarvis.shared.platform.fakeEmulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.platformName
import io.github.taetae98coding.jarvis.shared.settings.AppSettings
import io.github.taetae98coding.jarvis.shared.ui.KeepScreenAwakeTestTag
import kotlin.test.Test
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

        onNodeWithText("2 running / 5 total").assertIsDisplayed()
        onNodeWithText("0 running / 11 total").assertIsDisplayed()
    }

    @Test
    fun showsZeroEmulatorsWhenThePlatformCannotCountThem() = runComposeUiTest {
        setContent { App(InMemorySettingsStore(), fakeEmulatorProbe()) }

        onAllNodesWithText("0 running / 0 total").assertCountEquals(2)
    }
}
