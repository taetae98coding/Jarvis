package io.github.taetae98coding.jarvis.shared.settings

import io.github.taetae98coding.jarvis.shared.platform.InMemorySettingsStore
import io.github.taetae98coding.jarvis.shared.platform.SettingsPollInterval
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun keepScreenAwakeFollowsChangesWhenTheStoreHasNoCallback() = runTest {
        val store = InMemorySettingsStore(notifiesChanges = false)
        val settings = AppSettings(store, backgroundScope)
        runCurrent()

        store.putBoolean(AppSettings.KeepScreenAwakeKey, true)
        advanceTimeBy(SettingsPollInterval + 1.seconds)

        assertTrue(settings.keepScreenAwake.value)
    }

    @Test
    fun keepSystemScreenAwakeDefaultsToOff() = runTest {
        val settings = AppSettings(InMemorySettingsStore(), backgroundScope)

        assertFalse(settings.keepSystemScreenAwake.value)
    }

    @Test
    fun keepSystemScreenAwakeIsStoredUnderItsOwnKey() = runTest {
        val store = InMemorySettingsStore()
        val settings = AppSettings(store, backgroundScope)

        settings.setKeepSystemScreenAwake(true)

        assertTrue(store.getBoolean(AppSettings.KeepSystemScreenAwakeKey, false))
        assertFalse(store.getBoolean(AppSettings.KeepScreenAwakeKey, false))
    }
}
