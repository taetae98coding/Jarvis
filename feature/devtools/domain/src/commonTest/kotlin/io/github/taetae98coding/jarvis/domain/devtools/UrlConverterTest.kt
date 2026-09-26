package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlConverterTest {
    @Test
    fun encodesEverythingButUnreserved() {
        assertEquals(
            "%EC%95%88%EB%85%95%20a%2Bb%2Fc%3Fd%3De%26f~_.-%21",
            UrlConverter.encode("안녕 a+b/c?d=e&f~_.-!"),
        )
    }

    @Test
    fun decodesPercentAndRawCharacters() {
        assertEquals(DevToolValue.Text("안녕 a+b/c"), UrlConverter.decode("%EC%95%88%eb%85%95%20a+b/c"))
        assertEquals(DevToolValue.Text("안녕 👋"), UrlConverter.decode("안녕%20👋"))
    }

    @Test
    fun plusStaysPlus() {
        assertEquals(DevToolValue.Text("a+b"), UrlConverter.decode("a+b"))
    }

    @Test
    fun roundTrip() {
        val text = "https://example.com/경로?q=값&x=1 2#frag"
        assertEquals(DevToolValue.Text(text), UrlConverter.decode(UrlConverter.encode(text)))
    }

    @Test
    fun invalidPercent() {
        assertEquals(DevToolValue.Error(DevToolError.InvalidPercentEncoding(2)), UrlConverter.decode("ab%zz"))
        assertEquals(DevToolValue.Error(DevToolError.InvalidPercentEncoding(1)), UrlConverter.decode("a%4"))
    }

    @Test
    fun invalidUtf8() {
        assertEquals(DevToolValue.Error(DevToolError.InvalidUtf8), UrlConverter.decode("%FF"))
    }

    @Test
    fun convertShowsBothDirections() {
        assertEquals(
            listOf(
                DevToolOutput(DevToolOutputKind.URL_ENCODED, DevToolValue.Text("a%25"),),
                DevToolOutput(DevToolOutputKind.URL_DECODED, DevToolValue.Error(DevToolError.InvalidPercentEncoding(1))),
            ),
            UrlConverter.convert("a%"),
        )
        assertEquals(emptyList(), UrlConverter.convert(""))
    }
}
