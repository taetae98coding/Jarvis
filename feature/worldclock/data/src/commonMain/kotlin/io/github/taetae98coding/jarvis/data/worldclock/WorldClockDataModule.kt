package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.worldclock.ClockRepository
import io.github.taetae98coding.jarvis.domain.worldclock.SavedCitiesRepository
import io.github.taetae98coding.jarvis.domain.worldclock.TimeZoneRepository
import org.koin.dsl.module

val worldClockDataModule = module {
    single<ClockRepository> { SystemClockRepository }
    single<TimeZoneRepository> { PlatformTimeZoneRepository(systemZoneChanges(get())) }
    single<SavedCitiesRepository> { DefaultSavedCitiesRepository(createSettingsStore(get())) }
}
