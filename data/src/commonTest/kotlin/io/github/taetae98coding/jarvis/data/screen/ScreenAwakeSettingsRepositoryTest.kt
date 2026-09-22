package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.screen.DefaultScreenAwakeSettingsRepository.Companion.KeepScreenAwakeKey
import io.github.taetae98coding.jarvis.data.screen.DefaultScreenAwakeSettingsRepository.Companion.KeepSystemScreenAwakeKey
import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.data.settings.SettingsPollInterval
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenAwakeSettingsRepositoryTest {
    @Test
    fun keepScreenAwakeDefaultsToOff() = runTest {
        val settings = DefaultScreenAwakeSettingsRepository(InMemorySettingsStore(), backgroundScope)

        assertFalse(settings.keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeReadsStoredValue() = runTest {
        val store = InMemorySettingsStore()
        store.putBoolean(KeepScreenAwakeKey, true)

        assertTrue(DefaultScreenAwakeSettingsRepository(store, backgroundScope).keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeWritesThroughToStore() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultScreenAwakeSettingsRepository(store, backgroundScope)

        settings.setKeepScreenAwake(true)

        assertTrue(store.getBoolean(KeepScreenAwakeKey, false))
    }

    @Test
    fun keepScreenAwakeFollowsChangesMadeOutsideTheApp() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultScreenAwakeSettingsRepository(store, backgroundScope)
        runCurrent()

        store.putBoolean(KeepScreenAwakeKey, true)
        runCurrent()

        assertTrue(settings.keepScreenAwake.value)
    }

    @Test
    fun keepScreenAwakeFollowsChangesWhenTheStoreHasNoCallback() = runTest {
        val store = InMemorySettingsStore(notifiesChanges = false)
        val settings = DefaultScreenAwakeSettingsRepository(store, backgroundScope)
        runCurrent()

        store.putBoolean(KeepScreenAwakeKey, true)
        advanceTimeBy(SettingsPollInterval + 1.seconds)

        assertTrue(settings.keepScreenAwake.value)
    }

    @Test
    fun keepSystemScreenAwakeDefaultsToOff() = runTest {
        val settings = DefaultScreenAwakeSettingsRepository(InMemorySettingsStore(), backgroundScope)

        assertFalse(settings.keepSystemScreenAwake.value)
    }

    @Test
    fun keepSystemScreenAwakeIsStoredUnderItsOwnKey() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultScreenAwakeSettingsRepository(store, backgroundScope)

        settings.setKeepSystemScreenAwake(true)

        assertTrue(store.getBoolean(KeepSystemScreenAwakeKey, false))
        assertFalse(store.getBoolean(KeepScreenAwakeKey, false))
    }
}
