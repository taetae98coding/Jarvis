package io.github.taetae98coding.jarvis.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun platformNameIsNotBlank() {
        assertTrue(platformName.isNotBlank(), "platformName must be provided by every target")
    }
}
