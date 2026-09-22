package io.github.taetae98coding.jarvis.data.appinfo

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformNameTest {
    @Test
    fun platformNameIsNotBlank() {
        assertTrue(platformName.isNotBlank(), "모든 타깃은 platformName 을 제공해야 한다")
    }
}
