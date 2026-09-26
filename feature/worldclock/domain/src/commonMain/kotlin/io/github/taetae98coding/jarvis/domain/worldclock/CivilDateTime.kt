package io.github.taetae98coding.jarvis.domain.worldclock

/** 시간대 없는 날짜와 시각. 초 아래는 다루지 않는다. */
data class CivilDateTime(
    val date: CivilDate,
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
) : Comparable<CivilDateTime> {
    init {
        require(hour in 0..23 && minute in 0..59 && second in 0..59) { "$hour:$minute:$second" }
    }

    val time: TimeOfDay get() = TimeOfDay(hour, minute, second)

    /** 이 날짜·시각을 UTC 로 읽었을 때의 Unix 초. 시간대 오프셋을 빼면 진짜 순간이 된다. */
    val localEpochSeconds: Long
        get() = date.epochDay * SecondsPerDay + hour * 3600L + minute * 60L + second

    override fun compareTo(other: CivilDateTime): Int = localEpochSeconds.compareTo(other.localEpochSeconds)

    companion object {
        fun fromLocalEpochSeconds(seconds: Long): CivilDateTime {
            val secondOfDay = seconds.mod(SecondsPerDay).toInt()
            return CivilDateTime(
                date = CivilDate.fromEpochDay(seconds.floorDiv(SecondsPerDay)),
                hour = secondOfDay / 3600,
                minute = secondOfDay / 60 % 60,
                second = secondOfDay % 60,
            )
        }

        fun of(date: CivilDate, time: TimeOfDay): CivilDateTime = CivilDateTime(date, time.hour, time.minute, time.second)
    }
}

data class TimeOfDay(
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
) {
    override fun toString(): String = "${hour.pad2()}:${minute.pad2()}"

    companion object {
        /** `14:30`, `9:05`, `1430`, `14:30:15` 을 받는다. 범위를 벗어나면 null. 초가 없으면 0 이다. */
        fun parse(text: String): TimeOfDay? {
            val trimmed = text.trim()
            val parts = if (trimmed.length == 4 && trimmed.all(Char::isDigit)) {
                listOf(trimmed.substring(0, 2), trimmed.substring(2, 4))
            } else {
                trimmed.split(':').map(String::trim)
            }
            if (parts.size !in 2..3 || parts.any { it.isEmpty() || it.length > 2 || !it.all(Char::isDigit) }) return null

            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val second = parts.getOrNull(2)?.toInt() ?: 0
            if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null

            return TimeOfDay(hour, minute, second)
        }
    }
}

internal const val SecondsPerDay = 86_400L
