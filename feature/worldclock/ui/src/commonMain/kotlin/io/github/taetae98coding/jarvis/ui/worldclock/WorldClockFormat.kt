package io.github.taetae98coding.jarvis.ui.worldclock

import io.github.taetae98coding.jarvis.domain.worldclock.CivilDate
import io.github.taetae98coding.jarvis.domain.worldclock.CivilDateTime
import io.github.taetae98coding.jarvis.domain.worldclock.City
import io.github.taetae98coding.jarvis.domain.worldclock.DateDifference
import io.github.taetae98coding.jarvis.domain.worldclock.DayOfWeek
import kotlin.math.absoluteValue

// 음수 표시는 하이픈이 아닌 빼기 기호(U+2212)다. 글꼴에서 + 와 폭이 같아 줄이 흔들리지 않는다.
private const val Minus = "−"

internal fun CivilDateTime.clockText(withSeconds: Boolean): String =
    if (withSeconds) "${hour.pad2()}:${minute.pad2()}:${second.pad2()}" else "${hour.pad2()}:${minute.pad2()}"

/** `9월 27일 (일)` */
internal fun CivilDate.shortLabel(): String = "${month}월 ${day}일 (${dayOfWeek.label})"

/** `2026-09-27 (일)` */
internal fun CivilDate.isoLabel(): String = "$this (${dayOfWeek.label})"

internal val DayOfWeek.label: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

/** `UTC+9`, `UTC+5:45`, `UTC−3:30`, `UTC+0` */
internal fun utcOffsetLabel(offsetSeconds: Int): String {
    val sign = if (offsetSeconds < 0) Minus else "+"
    val minutes = offsetSeconds.absoluteValue / 60
    val hours = minutes / 60
    val rest = minutes % 60

    return if (rest == 0) "UTC$sign$hours" else "UTC$sign$hours:${rest.pad2()}"
}

/** 기준과의 시차. `+13시간`, `−3시간 15분`, `같은 시각` */
internal fun offsetDifferenceLabel(differenceSeconds: Int): String {
    if (differenceSeconds == 0) return "같은 시각"

    val sign = if (differenceSeconds < 0) Minus else "+"
    val minutes = differenceSeconds.absoluteValue / 60
    val hours = minutes / 60
    val rest = minutes % 60

    return when {
        hours == 0 -> "$sign${rest}분"
        rest == 0 -> "$sign${hours}시간"
        else -> "$sign${hours}시간 ${rest}분"
    }
}

/** 기준 날짜와 다를 때만 `+1일`·`−1일`. */
internal fun dayDifferenceLabel(days: Int): String? = when {
    days > 0 -> "+${days}일"
    days < 0 -> "$Minus${days.absoluteValue}일"
    else -> null
}

/** 남으면 `D-90`, 당일은 `D-Day`, 지나면 `D+26`. 한국에서 널리 쓰는 표기다. */
internal fun dDayLabel(daysUntil: Long): String = when {
    daysUntil > 0 -> "D-$daysUntil"
    daysUntil < 0 -> "D+${daysUntil.absoluteValue}"
    else -> "D-Day"
}

internal fun DateDifference.lines(): List<String> {
    val sign = if (isNegative) Minus else ""

    return listOf(
        "$sign${totalDays.absoluteValue.grouped()}일",
        "$sign${weeks.grouped()}주 ${weekDays}일",
        "$sign${years}년 ${months}개월 ${days}일",
    )
}

internal fun City.pickerLabel(): String = if (name == zoneId) zoneId else "$name ($zoneId)"

/** 세 자리마다 쉼표. 로캘을 따르지 않는다 — 공통 코드에는 로캘별 숫자 서식이 없다. */
internal fun Long.grouped(): String {
    val digits = absoluteValue.toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (this < 0) "$Minus$grouped" else grouped
}

private fun Int.pad2(): String = toString().padStart(2, '0')
