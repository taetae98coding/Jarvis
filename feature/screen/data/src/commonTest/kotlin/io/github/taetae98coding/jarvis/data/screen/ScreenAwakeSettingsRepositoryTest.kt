package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.screen.DefaultScreenAwakeSettingsRepository.Companion.KeepScreenAwakeKey
import io.github.taetae98coding.jarvis.data.screen.DefaultScreenAwakeSettingsRepository.Companion.KeepSystemScreenAwakeKey
import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.data.settings.SettingsPollInterval
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenAwakeSettingsRepositoryTest {
    @Test
    fun keepScreenAwakeDefaultsToOff() = runTest {
        val settings = DefaultScreenAwakeSettingsRepository(InMemorySettingsStore())

        assertFalse(settings.readKeepScreenAwake())
        assertFalse(settings.observeKeepScreenAwake().first())
    }

    @Test
    fun keepScreenAwakeReadsStoredValue() = runTest {
        val store = InMemorySettingsStore()
        store.putBoolean(KeepScreenAwakeKey, true)
        val settings = DefaultScreenAwakeSettingsRepository(store)

        assertTrue(settings.readKeepScreenAwake())
        assertTrue(settings.observeKeepScreenAwake().first())
    }

    @Test
    fun keepScreenAwakeWritesThroughToStore() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultScreenAwakeSettingsRepository(store)

        settings.setKeepScreenAwake(true)

        assertTrue(store.getBoolean(KeepScreenAwakeKey, false))
    }

    @Test
    fun keepScreenAwakeFollowsChangesMadeOutsideTheApp() = runTest {
        val store = InMemorySettingsStore()
        val values = mutableListOf<Boolean>()
        backgroundScope.launch { DefaultScreenAwakeSettingsRepository(store).observeKeepScreenAwake().toList(values) }
        runCurrent()

        store.putBoolean(KeepScreenAwakeKey, true)
        runCurrent()

        assertEquals(listOf(false, true), values)
    }

    @Test
    fun keepScreenAwakeFollowsChangesWhenTheStoreHasNoCallback() = runTest {
        val store = InMemorySettingsStore(notifiesChanges = false)
        val values = mutableListOf<Boolean>()
        backgroundScope.launch { DefaultScreenAwakeSettingsRepository(store).observeKeepScreenAwake().toList(values) }
        runCurrent()

        store.putBoolean(KeepScreenAwakeKey, true)
        advanceTimeBy(SettingsPollInterval + 1.seconds)

        assertEquals(listOf(false, true), values)
    }

    @Test
    fun listensToTheStoreOnlyWhileCollected() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultScreenAwakeSettingsRepository(store)
        runCurrent()
        assertEquals(0, store.listeners)

        val job = backgroundScope.launch { settings.observeKeepScreenAwake().collect {} }
        runCurrent()
        assertEquals(1, store.listeners)

        job.cancel()
        runCurrent()
        assertEquals(0, store.listeners)
    }

    @Test
    fun keepSystemScreenAwakeDefaultsToOff() = runTest {
        val settings = DefaultScreenAwakeSettingsRepository(InMemorySettingsStore())

        assertFalse(settings.readKeepSystemScreenAwake())
    }

    @Test
    fun keepSystemScreenAwakeIsStoredUnderItsOwnKey() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultScreenAwakeSettingsRepository(store)

        settings.setKeepSystemScreenAwake(true)

        assertTrue(store.getBoolean(KeepSystemScreenAwakeKey, false))
        assertFalse(store.getBoolean(KeepScreenAwakeKey, false))
    }
}
