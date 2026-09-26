package io.github.taetae98coding.jarvis.domain.worldclock

/**
 * 시간대 없는 달력 날짜. 1582년 이전에도 그레고리력을 그대로 늘려 쓴다(proleptic).
 *
 * 날 수 변환은 Howard Hinnant 의 `days_from_civil`·`civil_from_days` 다. 400년(146,097일) 주기로 윤년 규칙이
 * 되풀이되는 것을 이용해 반복문 없이 계산한다.
 */
data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<CivilDate> {
    init {
        require(month in 1..12) { "month $month" }
        require(day in 1..lengthOfMonth(year, month)) { "day $day of $year-$month" }
    }

    /** 1970-01-01 부터 센 날 수. */
    val epochDay: Long
        get() {
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = (if (y >= 0) y else y - 399) / 400
            val yearOfEra = y - era * 400
            val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return era * DaysPerEra + dayOfEra - EpochShift
        }

    val dayOfWeek: DayOfWeek
        // 1970-01-01 은 목요일이다.
        get() = DayOfWeek.entries[(epochDay + 3).mod(7)]

    val isLeapYear: Boolean get() = isLeapYear(year)

    fun plusDays(days: Long): CivilDate = fromEpochDay(epochDay + days)

    /** 없는 날(예: 1월 31일 + 1개월)은 그 달의 말일로 당긴다. */
    fun plusMonths(months: Long): CivilDate {
        val total = year * 12L + (month - 1) + months
        val newYear = total.floorDiv(12).toInt()
        val newMonth = total.mod(12) + 1
        return CivilDate(newYear, newMonth, minOf(day, lengthOfMonth(newYear, newMonth)))
    }

    override fun compareTo(other: CivilDate): Int = epochDay.compareTo(other.epochDay)

    /** ISO-8601 `YYYY-MM-DD`. */
    override fun toString(): String =
        "${year.toString().padStart(4, '0')}-${month.pad2()}-${day.pad2()}"

    companion object {
        fun fromEpochDay(epochDay: Long): CivilDate {
            val z = epochDay + EpochShift
            val era = (if (z >= 0) z else z - (DaysPerEra - 1)) / DaysPerEra
            val dayOfEra = z - era * DaysPerEra
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val mp = (5 * dayOfYear + 2) / 153
            val day = (dayOfYear - (153 * mp + 2) / 5 + 1).toInt()
            val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
            val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
            return CivilDate(year, month, day)
        }

        fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        /**
         * `2026-09-26`, `2026.9.26`, `2026/09/26`, `20260926` 을 받는다. 연도는 1–9999 다.
         * 없는 날(2월 30일)이나 다른 모양이면 null.
         */
        fun parse(text: String): CivilDate? {
            val trimmed = text.trim().removeSuffix(".")
            val parts = if (trimmed.length == 8 && trimmed.all(Char::isDigit)) {
                listOf(trimmed.substring(0, 4), trimmed.substring(4, 6), trimmed.substring(6, 8))
            } else {
                trimmed.split('-', '.', '/').map(String::trim)
            }
            if (parts.size != 3 || parts.any { it.isEmpty() || !it.all(Char::isDigit) || it.length > 4 }) return null

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            if (year !in 1..9999 || month !in 1..12 || day !in 1..lengthOfMonth(year, month)) return null

            return CivilDate(year, month, day)
        }
    }
}

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY,
}

/** 0001-03-01 을 기준으로 센 날 수와 1970-01-01 기준 날 수의 차이. */
private const val EpochShift = 719_468L

private const val DaysPerEra = 146_097L

internal fun Int.pad2(): String = toString().padStart(2, '0')
