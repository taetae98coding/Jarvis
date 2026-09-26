package io.github.taetae98coding.jarvis.ui.worldclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.worldclock.AddCityUseCase
import io.github.taetae98coding.jarvis.domain.worldclock.CalculateDatesUseCase
import io.github.taetae98coding.jarvis.domain.worldclock.City
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDate
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDateTime
import io.github.taetae98coding.jarvis.domain.worldclock.ConvertTimeUseCase
import io.github.taetae98coding.jarvis.domain.worldclock.DateDifference
import io.github.taetae98coding.jarvis.domain.worldclock.InternationalAge
import io.github.taetae98coding.jarvis.domain.worldclock.ObserveWorldClockUseCase
import io.github.taetae98coding.jarvis.domain.worldclock.RemoveCityUseCase
import io.github.taetae98coding.jarvis.domain.worldclock.SearchCitiesUseCase
import io.github.taetae98coding.jarvis.domain.worldclock.TimeConversion
import io.github.taetae98coding.jarvis.domain.worldclock.TimeOfDay
import io.github.taetae98coding.jarvis.domain.worldclock.WorldCities
import io.github.taetae98coding.jarvis.domain.worldclock.WorldClockState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class WorldClockCardViewModel(
    observeWorldClock: ObserveWorldClockUseCase,
) : ViewModel() {
    val state: StateFlow<WorldClockState> = observeWorldClock(viewModelScope)
}

/**
 * 화면의 입력 칸·탭·고른 시간대는 화면이 `rememberSaveable` 로 든다. 여기의 계산 함수는 입력 글자만 받는 순수
 * 함수라, 화면이 입력이 바뀔 때 `remember` 로 다시 부른다.
 */
internal class WorldClockViewModel(
    observeWorldClock: ObserveWorldClockUseCase,
    private val addCity: AddCityUseCase,
    private val removeCity: RemoveCityUseCase,
    private val searchCities: SearchCitiesUseCase,
    private val convertTime: ConvertTimeUseCase,
    private val calculateDates: CalculateDatesUseCase,
) : ViewModel() {
    val state: StateFlow<WorldClockState> = observeWorldClock(viewModelScope)

    fun onAddCity(zoneId: String) {
        viewModelScope.launch { addCity(zoneId) }
    }

    fun onRemoveCity(zoneId: String) {
        viewModelScope.launch { removeCity(zoneId) }
    }

    fun candidates(query: String, saved: List<String>): List<City> = searchCities(query, exclude = saved)

    /** 시간대 변환에서 고를 수 있는 시간대. 기기 시간대가 목록에 없으면 맨 앞에 붙인다. */
    fun zoneOptions(localZoneId: String): List<City> {
        val all = searchCities.all()
        val local = WorldCities.cityOf(localZoneId)
        return if (all.any { it.zoneId == local.zoneId }) all else listOf(local) + all
    }

    fun convert(dateText: String, timeText: String, fromZoneId: String, toZoneIds: List<String>): ConversionResult {
        val date = CivilDate.parse(dateText) ?: return ConversionResult.InvalidInput
        val time = TimeOfDay.parse(timeText) ?: return ConversionResult.InvalidInput
        val conversion = convertTime(CivilDateTime.of(date, time), fromZoneId, toZoneIds) ?: return ConversionResult.UnknownZone

        return ConversionResult.Converted(conversion)
    }

    fun nowIn(zoneId: String): CivilDateTime? = convertTime.at(state.value.now, zoneId)?.dateTime

    fun difference(fromText: String, toText: String): DateDifference? {
        val from = CivilDate.parse(fromText) ?: return null
        val to = CivilDate.parse(toText) ?: return null
        return calculateDates.difference(from, to)
    }

    fun addDays(baseText: String, daysText: String): CivilDate? {
        val base = CivilDate.parse(baseText) ?: return null
        val days = daysText.trim().replace('−', '-').removePrefix("+").toLongOrNull()?.takeIf { it in DaysLimit } ?: return null
        return runCatching { calculateDates.addDays(base, days) }.getOrNull()?.takeIf { it.year in 1..9999 }
    }

    fun age(birthText: String, today: CivilDate): AgeResult? {
        val birth = CivilDate.parse(birthText) ?: return null
        return calculateDates.internationalAge(birth, today)?.let(AgeResult::Age) ?: AgeResult.NotBornYet
    }

    fun daysUntil(targetText: String, today: CivilDate): Pair<CivilDate, Long>? {
        val target = CivilDate.parse(targetText) ?: return null
        return target to calculateDates.daysUntil(target, today)
    }
}

internal sealed interface ConversionResult {
    data object InvalidInput : ConversionResult

    data object UnknownZone : ConversionResult

    data class Converted(val conversion: TimeConversion) : ConversionResult
}

internal sealed interface AgeResult {
    data class Age(val age: InternationalAge) : AgeResult

    data object NotBornYet : AgeResult
}

// 1–9999 년 밖으로는 날짜를 쓰지 않는다. 그 폭(약 365 만 일)보다 큰 수는 계산하기 전에 막는다.
private val DaysLimit = -3_700_000L..3_700_000L
