package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.math.absoluteValue
import kotlin.time.Instant

object TimestampConverter {
    /**
     * 이 값 이상이면 밀리초로 본다. 초로 읽으면 서기 5138 년이고, 밀리초로 읽으면 1973 년 3월이라
     * 지금 쓰이는 두 단위가 겹치지 않는다.
     */
    const val MillisecondsThreshold: Long = 100_000_000_000L

    fun convert(input: String): List<DevToolOutput> {
        val text = input.trim()
        if (text.isEmpty()) return emptyList()

        val number = text.toLongOrNull()
        val instant = when {
            number == null -> runCatching { Instant.parse(text) }.getOrNull()
            number.absoluteValue >= MillisecondsThreshold -> Instant.fromEpochMilliseconds(number)
            else -> Instant.fromEpochSeconds(number)
        } ?: return listOf(DevToolOutput(DevToolOutputKind.ISO_8601, DevToolValue.Error(DevToolError.InvalidTimestamp)))

        return buildList {
            if (number != null) {
                val unit = if (number.absoluteValue >= MillisecondsThreshold) TimestampUnit.MILLISECONDS else TimestampUnit.SECONDS
                add(DevToolOutput(DevToolOutputKind.TIMESTAMP_UNIT, DevToolValue.Text(unit.name)))
            }
            add(DevToolOutput(DevToolOutputKind.ISO_8601, DevToolValue.Text(instant.toString())))
            add(DevToolOutput(DevToolOutputKind.EPOCH_SECONDS, DevToolValue.Text(instant.epochSeconds.toString())))
            add(DevToolOutput(DevToolOutputKind.EPOCH_MILLISECONDS, DevToolValue.Text(instant.toEpochMilliseconds().toString())))
        }
    }
}

/** [DevToolOutputKind.TIMESTAMP_UNIT] 줄의 값은 이 이름이다. 화면이 한국어로 바꿔 적는다. */
enum class TimestampUnit {
    SECONDS,
    MILLISECONDS,
}
