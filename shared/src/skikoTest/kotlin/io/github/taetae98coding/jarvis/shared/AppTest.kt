package io.github.taetae98coding.jarvis.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.shared.platform.InMemorySettingsStore
import io.github.taetae98coding.jarvis.shared.platform.platformName
import io.github.taetae98coding.jarvis.shared.settings.AppSettings
import io.github.taetae98coding.jarvis.shared.ui.KeepScreenAwakeTestTag
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @Test
    fun showsAppVersion() = runComposeUiTest {
        setContent { App(InMemorySettingsStore()) }

        onNodeWithText(APP_VERSION).assertIsDisplayed()
    }

    @Test
    fun showsCurrentPlatformName() = runComposeUiTest {
        setContent { App(InMemorySettingsStore()) }

        onNodeWithText(platformName).assertIsDisplayed()
    }

    @Test
    fun keepScreenAwakeDefaultsToOff() = runComposeUiTest {
        setContent { App(InMemorySettingsStore()) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOff()
    }

    @Test
    fun keepScreenAwakeTogglesOn() = runComposeUiTest {
        setContent { App(InMemorySettingsStore()) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick().assertIsOn()
    }

    @Test
    fun keepScreenAwakeIsPersisted() = runComposeUiTest {
        val store = InMemorySettingsStore()
        setContent { App(store) }

        onNodeWithTag(KeepScreenAwakeTestTag).performClick()

        assertTrue(store.getBoolean(AppSettings.KeepScreenAwakeKey, false))
    }

    @Test
    fun keepScreenAwakeIsRestoredOnRelaunch() = runComposeUiTest {
        val store = InMemorySettingsStore()
        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)

        setContent { App(store) }

        onNodeWithTag(KeepScreenAwakeTestTag).assertIsOn()
    }
}
