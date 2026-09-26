package io.github.taetae98coding.jarvis.domain.worldclock

import kotlin.math.absoluteValue

/**
 * 두 날짜 사이. [totalDays] 는 [to] − [from] 이라 [to] 가 앞이면 음수다. 나머지 필드는 크기이고 방향은
 * [totalDays] 의 부호가 말한다.
 */
data class DateDifference(
    val totalDays: Long,
    val weeks: Long,
    val weekDays: Int,
    val years: Int,
    val months: Int,
    val days: Int,
) {
    val isNegative: Boolean get() = totalDays < 0
}

/** 만 나이와 그 나이가 하나 느는 다음 날. */
data class InternationalAge(
    val years: Int,
    val nextBirthday: CivilDate,
    val daysUntilNextBirthday: Long,
)

object DateCalculator {
    /**
     * 연·월·일 차이는 `java.time.Period.between` 과 같게 센다. 앞 날짜에서 달을 더해 가다 넘치기 직전의 달까지를
     * 온전한 달로 치고, 없는 날은 그 달 말일로 당긴다(1월 31일 + 1개월 = 2월 28일).
     */
    fun difference(from: CivilDate, to: CivilDate): DateDifference {
        val totalDays = to.epochDay - from.epochDay
        val (start, end) = if (totalDays >= 0) from to to else to to from

        var totalMonths = (end.year * 12L + end.month) - (start.year * 12L + start.month)
        var days = end.day - start.day
        if (totalMonths > 0 && days < 0) {
            totalMonths--
            days = (end.epochDay - start.plusMonths(totalMonths).epochDay).toInt()
        }

        val magnitude = totalDays.absoluteValue
        return DateDifference(
            totalDays = totalDays,
            weeks = magnitude / 7,
            weekDays = (magnitude % 7).toInt(),
            years = (totalMonths / 12).toInt(),
            months = (totalMonths % 12).toInt(),
            days = days,
        )
    }

    fun addDays(date: CivilDate, days: Long): CivilDate = date.plusDays(days)

    /**
     * 2023-06-28 부터 법령·계약의 나이는 만 나이다(민법 제158조). 출생일을 넣어 세므로 생일 당일에 한 살을 먹고,
     * 2월 29일생은 평년에 해당일이 없어 2월 말일로 기간이 끝나므로(민법 제160조 제3항) 3월 1일에 먹는다.
     * [today] 가 [birth] 보다 앞이면 null.
     */
    fun internationalAge(birth: CivilDate, today: CivilDate): InternationalAge? {
        if (today < birth) return null

        val thisYear = birthdayIn(birth, today.year)
        val years = today.year - birth.year - if (today < thisYear) 1 else 0
        val next = if (today < thisYear) thisYear else birthdayIn(birth, today.year + 1)

        return InternationalAge(years = years, nextBirthday = next, daysUntilNextBirthday = next.epochDay - today.epochDay)
    }

    /** [target] − [today] 날 수. 0 이 D-Day, 양수가 남은 날(D−n), 음수가 지난 날(D+n)이다. */
    fun daysUntil(target: CivilDate, today: CivilDate): Long = target.epochDay - today.epochDay

    private fun birthdayIn(birth: CivilDate, year: Int): CivilDate =
        if (birth.month == 2 && birth.day == 29 && !CivilDate.isLeapYear(year)) {
            CivilDate(year, 3, 1)
        } else {
            CivilDate(year, birth.month, birth.day)
        }
}
