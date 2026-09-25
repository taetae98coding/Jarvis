package io.github.taetae98coding.jarvis.ui.profiling

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfilingFormatTest {
    @Test
    fun percentIsRoundedAndClamped() {
        assertEquals("23%", formatPercent(22.6))
        assertEquals("0%", formatPercent(-3.0))
        assertEquals("100%", formatPercent(140.0))
    }

    @Test
    fun bytesUseDecimalUnitsWithOneDecimalBelowOneHundred() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("999 B", formatBytes(999))
        assertEquals("1.2 KB", formatBytes(1_234))
        assertEquals("34.4 GB", formatBytes(34_359_738_368))
        assertEquals("30 KB", formatBytes(30_000))
        assertEquals("27.6 GB", formatBytes(27_600_000_000))
        assertEquals("494 GB", formatBytes(494_384_795_648))
        assertEquals("2 TB", formatBytes(2_000_000_000_000))
    }

    @Test
    fun wholeNumbersDropTheDecimal() {
        assertEquals("10 MB", formatBytes(9_960_000))
        assertEquals("100 MB", formatBytes(99_960_000))
    }

    @Test
    fun bytesPerSecondAppendsTheRate() {
        assertEquals("1.2 MB/s", formatBytesPerSecond(1_200_000))
        assertEquals("30 KB/s", formatBytesPerSecond(30_000))
    }
}
