package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.test.Test
import kotlin.test.assertEquals

class Base64ConverterTest {
    @Test
    fun encodesUtf8() {
        assertEquals("7JWI64WVIHdvcmxkPz4=", Base64Converter.encode("안녕 world?>", urlSafe = false))
    }

    @Test
    fun urlSafeAlphabet() {
        // "?>?" 는 0xFB 0xFF 처럼 + / 가 나오는 바이트를 만든다.
        assertEquals("Pz4_", Base64Converter.encode("?>?", urlSafe = true))
        assertEquals("Pz4/", Base64Converter.encode("?>?", urlSafe = false))
    }

    @Test
    fun roundTrip() {
        listOf("", "a", "ab", "abc", "안녕하세요 👋", "{\"key\":\"value\"}").forEach { text ->
            listOf(false, true).forEach { urlSafe ->
                assertEquals(DevToolValue.Text(text), Base64Converter.decode(Base64Converter.encode(text, urlSafe), urlSafe))
            }
        }
    }

    @Test
    fun decodeAcceptsMissingPaddingAndWhitespace() {
        assertEquals(DevToolValue.Text("안녕 world?>"), Base64Converter.decode("7JWI64WV\nIHdvcmxk Pz4", urlSafe = false))
    }

    @Test
    fun invalidBase64() {
        assertEquals(DevToolValue.Error(DevToolError.InvalidBase64), Base64Converter.decode("abc*", urlSafe = false))
        // URL-safe 가 아닌 알파벳은 URL-safe 모드에서 틀린다.
        assertEquals(DevToolValue.Error(DevToolError.InvalidBase64), Base64Converter.decode("Pz4/", urlSafe = true))
    }

    @Test
    fun decodedBytesMustBeUtf8() {
        // 0xFB 0xFF
        assertEquals(DevToolValue.Error(DevToolError.InvalidUtf8), Base64Converter.decode("+/8=", urlSafe = false))
    }

    @Test
    fun convertShowsBothDirections() {
        assertEquals(
            listOf(
                DevToolOutput(DevToolOutputKind.BASE64_ENCODED, DevToolValue.Text("Kg==")),
                DevToolOutput(DevToolOutputKind.BASE64_DECODED, DevToolValue.Error(DevToolError.InvalidBase64)),
            ),
            Base64Converter.convert("*", urlSafe = false),
        )
        assertEquals(emptyList(), Base64Converter.convert("", urlSafe = false))
    }
}
