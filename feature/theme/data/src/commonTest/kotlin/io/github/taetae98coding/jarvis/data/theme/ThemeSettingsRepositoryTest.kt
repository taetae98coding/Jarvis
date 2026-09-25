package io.github.taetae98coding.jarvis.data.theme

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.data.settings.SettingsPollInterval
import io.github.taetae98coding.jarvis.data.theme.DefaultThemeSettingsRepository.Companion.ThemeModeKey
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsRepositoryTest {
    @Test
    fun themeModeDefaultsToSystem() = runTest {
        val settings = DefaultThemeSettingsRepository(InMemorySettingsStore())

        assertEquals(ThemeMode.SYSTEM, settings.readThemeMode())
        assertEquals(ThemeMode.SYSTEM, settings.observeThemeMode().first())
    }

    @Test
    fun themeModeReadsStoredValueAndIgnoresUnknownOnes() = runTest {
        val store = InMemorySettingsStore()
        store.putString(ThemeModeKey, "dark")
        val settings = DefaultThemeSettingsRepository(store)
        assertEquals(ThemeMode.DARK, settings.readThemeMode())

        store.putString(ThemeModeKey, "purple")
        assertEquals(ThemeMode.SYSTEM, settings.readThemeMode())
    }

    @Test
    fun themeModeWritesThroughToStore() = runTest {
        val store = InMemorySettingsStore()

        DefaultThemeSettingsRepository(store).setThemeMode(ThemeMode.LIGHT)

        assertEquals("light", store.getString(ThemeModeKey, ""))
    }

    @Test
    fun themeModeFollowsChangesMadeOutsideTheApp() = runTest {
        val store = InMemorySettingsStore()
        val values = mutableListOf<ThemeMode>()
        backgroundScope.launch { DefaultThemeSettingsRepository(store).observeThemeMode().toList(values) }
        runCurrent()

        store.putString(ThemeModeKey, "dark")
        runCurrent()

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.DARK), values)
    }

    @Test
    fun themeModeFollowsChangesWhenTheStoreHasNoCallback() = runTest {
        val store = InMemorySettingsStore(notifiesChanges = false)
        val values = mutableListOf<ThemeMode>()
        backgroundScope.launch { DefaultThemeSettingsRepository(store).observeThemeMode().toList(values) }
        runCurrent()

        store.putString(ThemeModeKey, "light")
        advanceTimeBy(SettingsPollInterval + 1.seconds)

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT), values)
    }

    @Test
    fun listensToTheStoreOnlyWhileCollected() = runTest {
        val store = InMemorySettingsStore()
        val settings = DefaultThemeSettingsRepository(store)
        val job = backgroundScope.launch { settings.observeThemeMode().collect {} }
        runCurrent()
        assertEquals(1, store.listeners)

        job.cancel()
        runCurrent()
        assertEquals(0, store.listeners)
    }
}
