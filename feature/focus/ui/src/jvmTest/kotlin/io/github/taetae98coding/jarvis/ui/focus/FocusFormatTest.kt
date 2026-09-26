package io.github.taetae98coding.jarvis.ui.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FocusFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("25:00", formatRemaining(25.minutes))
        assertEquals("04:07", formatRemaining(4.minutes + 7.seconds))
        assertEquals("00:00", formatRemaining(Duration.ZERO))
        assertEquals("00:00", formatRemaining((-3).seconds))
    }
}
