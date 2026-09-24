package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrowserAddressTest {
    @Test
    fun blankInputIsNothing() {
        assertNull(browserAddress("   "))
    }

    @Test
    fun addressWithASchemeIsKept() {
        assertEquals("http://example.com/a?b=c", browserAddress("  http://example.com/a?b=c "))
        assertEquals("file:///tmp/a.html", browserAddress("file:///tmp/a.html"))
    }

    @Test
    fun hostGetsHttps() {
        assertEquals("https://github.com/taetae98coding", browserAddress("github.com/taetae98coding"))
    }

    @Test
    fun localhostGetsHttp() {
        assertEquals("http://localhost:8080", browserAddress("localhost:8080"))
    }

    @Test
    fun everythingElseIsASearch() {
        assertEquals("https://www.google.com/search?q=kotlin", browserAddress("kotlin"))
        assertEquals("https://www.google.com/search?q=compose%20webview%201.0", browserAddress("compose webview 1.0"))
        assertEquals("https://www.google.com/search?q=%ED%84%B0%EB%AF%B8%EB%84%90", browserAddress("터미널"))
    }
}
