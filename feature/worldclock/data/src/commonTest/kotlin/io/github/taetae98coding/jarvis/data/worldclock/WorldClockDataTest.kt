package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.settings.InMemorySettingsStore
import io.github.taetae98coding.jarvis.domain.worldclock.WorldCities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class WorldClockDataTest {
    @Test
    fun firstLaunchShowsDefaultCities() {
        assertEquals(WorldCities.defaultZoneIds, DefaultSavedCitiesRepository(InMemorySettingsStore()).readSavedZoneIds())
    }

    @Test
    fun savedOrderSurvivesNewRepository() {
        val store = InMemorySettingsStore()
        DefaultSavedCitiesRepository(store).setSavedZoneIds(listOf("Asia/Tokyo", "UTC", "America/Argentina/Buenos_Aires"))

        assertEquals(listOf("Asia/Tokyo", "UTC", "America/Argentina/Buenos_Aires"), DefaultSavedCitiesRepository(store).readSavedZoneIds())
        assertEquals("Asia/Tokyo,UTC,America/Argentina/Buenos_Aires", store.getString(DefaultSavedCitiesRepository.CitiesKey, ""))
    }

    @Test
    fun removingEveryCityKeepsTheListEmpty() {
        val store = InMemorySettingsStore()
        DefaultSavedCitiesRepository(store).setSavedZoneIds(emptyList())

        assertEquals(emptyList(), DefaultSavedCitiesRepository(store).readSavedZoneIds())
    }

    @Test
    fun malformedStoredValueIsCleanedUp() {
        val store = InMemorySettingsStore(mutableMapOf<String, Any>(DefaultSavedCitiesRepository.CitiesKey to " Asia/Tokyo,,Asia/Tokyo, UTC "))

        assertEquals(listOf("Asia/Tokyo", "UTC"), DefaultSavedCitiesRepository(store).readSavedZoneIds())
    }

    @Test
    fun observeFollowsWrites() = runTest {
        val repository = DefaultSavedCitiesRepository(InMemorySettingsStore())

        repository.setSavedZoneIds(listOf("Europe/Paris"))
        assertEquals(listOf("Europe/Paris"), repository.observeSavedZoneIds().first())
    }

    @Test
    fun localZoneFollowsChangeSignalAndReleasesIt() = runTest {
        var listeners = 0
        val changes = callbackFlow {
            listeners++
            send(Unit)
            awaitClose { listeners-- }
        }
        val repository = PlatformTimeZoneRepository(changes)

        assertEquals(systemZoneId(), repository.observeLocalZoneId().first())
        assertEquals(0, listeners)
    }

    @Test
    fun clockEmitsWholeSeconds() = runTest {
        val now = SystemClockRepository.readNow()

        assertEquals(now, Instant.fromEpochSeconds(now.epochSeconds))
    }
}
