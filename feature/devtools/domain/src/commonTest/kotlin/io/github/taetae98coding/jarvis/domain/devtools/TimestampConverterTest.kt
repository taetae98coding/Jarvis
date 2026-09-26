package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampConverterTest {
    @Test
    fun secondsToIso() {
        assertEquals(
            listOf(
                text(DevToolOutputKind.TIMESTAMP_UNIT, "SECONDS"),
                text(DevToolOutputKind.ISO_8601, "2023-11-14T22:13:20Z"),
                text(DevToolOutputKind.EPOCH_SECONDS, "1700000000"),
                text(DevToolOutputKind.EPOCH_MILLISECONDS, "1700000000000"),
            ),
            TimestampConverter.convert(" 1700000000\n"),
        )
    }

    @Test
    fun millisecondsToIso() {
        assertEquals(
            listOf(
                text(DevToolOutputKind.TIMESTAMP_UNIT, "MILLISECONDS"),
                text(DevToolOutputKind.ISO_8601, "2023-11-14T22:13:20.123Z"),
                text(DevToolOutputKind.EPOCH_SECONDS, "1700000000"),
                text(DevToolOutputKind.EPOCH_MILLISECONDS, "1700000000123"),
            ),
            TimestampConverter.convert("1700000000123"),
        )
    }

    @Test
    fun thresholdSeparatesUnits() {
        assertEquals(text(DevToolOutputKind.TIMESTAMP_UNIT, "SECONDS"), TimestampConverter.convert("99999999999").first())
        assertEquals(text(DevToolOutputKind.TIMESTAMP_UNIT, "MILLISECONDS"), TimestampConverter.convert("100000000000").first())
        assertEquals(text(DevToolOutputKind.ISO_8601, "1973-03-03T09:46:40Z"), TimestampConverter.convert("100000000000")[1])
    }

    @Test
    fun negativeSecondsBeforeEpoch() {
        assertEquals(text(DevToolOutputKind.ISO_8601, "1969-12-31T23:59:59Z"), TimestampConverter.convert("-1")[1])
    }

    @Test
    fun isoToEpoch() {
        assertEquals(
            listOf(
                text(DevToolOutputKind.ISO_8601, "2023-11-14T22:13:20Z"),
                text(DevToolOutputKind.EPOCH_SECONDS, "1700000000"),
                text(DevToolOutputKind.EPOCH_MILLISECONDS, "1700000000000"),
            ),
            TimestampConverter.convert("2023-11-15T07:13:20+09:00"),
        )
    }

    @Test
    fun invalidInput() {
        assertEquals(
            listOf(DevToolOutput(DevToolOutputKind.ISO_8601, DevToolValue.Error(DevToolError.InvalidTimestamp))),
            TimestampConverter.convert("yesterday"),
        )
    }

    @Test
    fun blankInputHasNoOutput() {
        assertEquals(emptyList(), TimestampConverter.convert("  "))
    }

    private fun text(kind: DevToolOutputKind, value: String) = DevToolOutput(kind, DevToolValue.Text(value))
}
