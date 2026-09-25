package io.github.taetae98coding.jarvis.domain.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeTest {
    @Test
    fun systemFollowsTheSystem() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDark = false))
    }

    @Test
    fun lightAndDarkIgnoreTheSystem() {
        assertFalse(ThemeMode.LIGHT.isDark(systemInDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemInDark = false))
    }

    @Test
    fun storedValueRoundTripsAndUnknownFallsBackToSystem() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStored(it.storedValue)) }
        assertEquals("dark", ThemeMode.DARK.storedValue)
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("purple"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
    }
}
