package io.github.taetae98coding.jarvis.shared.settings

import io.github.taetae98coding.jarvis.shared.platform.InMemorySettingsStore
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun keepScreenAwakeDefaultsToOff() = runTest {
        val settings = AppSettings(InMemorySettingsStore(), backgroundScope)

        assertFalse(settings.keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeReadsStoredValue() = runTest {
        val store = InMemorySettingsStore()
        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)

        assertTrue(AppSettings(store, backgroundScope).keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeWritesThroughToStore() = runTest {
        val store = InMemorySettingsStore()
        val settings = AppSettings(store, backgroundScope)

        settings.setKeepScreenAwake(true)

        assertTrue(store.getBoolean(AppSettings.KeepScreenAwakeKey, false))
    }

    @Test
    fun keepScreenAwakeFollowsChangesMadeOutsideTheApp() = runTest {
        val store = InMemorySettingsStore()
        val settings = AppSettings(store, backgroundScope)
        runCurrent()

        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)
        runCurrent()

        assertTrue(settings.keepScreenAwake.value)
    }
}
