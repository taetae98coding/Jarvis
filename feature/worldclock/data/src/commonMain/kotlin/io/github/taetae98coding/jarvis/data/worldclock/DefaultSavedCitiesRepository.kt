package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.worldclock.SavedCitiesRepository
import io.github.taetae98coding.jarvis.domain.worldclock.WorldCities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 시간대 이름을 쉼표로 이어 문자열 하나에 둔다. IANA 이름에는 쉼표가 없다.
 *
 * 키가 없으면(처음 설치) 기본 도시를, 빈 문자열이면(사용자가 모두 지움) 빈 목록을 준다.
 */
internal class DefaultSavedCitiesRepository(
    private val store: SettingsStore,
) : SavedCitiesRepository {
    override fun observeSavedZoneIds(): Flow<List<String>> = store.observeString(CitiesKey, DefaultValue).map(::decode)

    override fun readSavedZoneIds(): List<String> = decode(store.getString(CitiesKey, DefaultValue))

    override fun setSavedZoneIds(zoneIds: List<String>) {
        store.putString(CitiesKey, zoneIds.distinct().joinToString(Separator))
    }

    internal companion object {
        const val CitiesKey = "worldclock_cities"

        private const val Separator = ","

        private val DefaultValue = WorldCities.defaultZoneIds.joinToString(Separator)

        private fun decode(stored: String): List<String> = stored.split(Separator).map(String::trim).filter(String::isNotEmpty).distinct()
    }
}
