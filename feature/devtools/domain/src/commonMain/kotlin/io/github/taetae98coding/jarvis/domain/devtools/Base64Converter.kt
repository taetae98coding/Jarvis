package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.io.encoding.Base64

object Base64Converter {
    fun convert(input: String, urlSafe: Boolean): List<DevToolOutput> {
        if (input.isEmpty()) return emptyList()

        return listOf(
            DevToolOutput(DevToolOutputKind.BASE64_ENCODED, DevToolValue.Text(encode(input, urlSafe))),
            DevToolOutput(DevToolOutputKind.BASE64_DECODED, decode(input, urlSafe)),
        )
    }

    fun encode(text: String, urlSafe: Boolean): String = codec(urlSafe).encode(text.encodeToByteArray())

    fun decode(text: String, urlSafe: Boolean): DevToolValue {
        // 여러 줄로 접힌 Base64(PEM, 메일 본문)를 그대로 붙여 넣는 경우가 많다.
        val compact = text.filterNot(Char::isWhitespace)
        val bytes = runCatching { codec(urlSafe).decode(compact) }.getOrNull()
            ?: return DevToolValue.Error(DevToolError.InvalidBase64)

        return decodeUtf8(bytes)
    }

    private fun codec(urlSafe: Boolean): Base64 =
        (if (urlSafe) Base64.UrlSafe else Base64.Default).withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
}

internal fun decodeUtf8(bytes: ByteArray): DevToolValue =
    runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
        .fold(onSuccess = DevToolValue::Text, onFailure = { DevToolValue.Error(DevToolError.InvalidUtf8) })
