package io.github.taetae98coding.jarvis.shared.settings

import io.github.taetae98coding.jarvis.shared.platform.InMemorySettingsStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun keepScreenAwakeDefaultsToOff() {
        val settings = AppSettings(InMemorySettingsStore())

        assertFalse(settings.keepScreenAwake)
    }

    @Test
    fun keepScreenAwakeReadsStoredValue() {
        val store = InMemorySettingsStore()
        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)

        assertTrue(AppSettings(store).keepScreenAwake)
    }

    @Test
    fun keepScreenAwakeWritesThroughToStore() {
        val store = InMemorySettingsStore()
        val settings = AppSettings(store)

        settings.keepScreenAwake = true

        assertTrue(store.getBoolean(AppSettings.KeepScreenAwakeKey, false))
    }
}
