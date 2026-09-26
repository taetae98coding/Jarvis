package io.github.taetae98coding.jarvis.domain.devtools

/** RFC 3986 퍼센트 인코딩. `+` 를 공백으로 읽는 폼 인코딩 규칙은 따르지 않는다(docs/common/dev-utilities.html). */
object UrlConverter {
    fun convert(input: String): List<DevToolOutput> {
        if (input.isEmpty()) return emptyList()

        return listOf(
            DevToolOutput(DevToolOutputKind.URL_ENCODED, DevToolValue.Text(encode(input))),
            DevToolOutput(DevToolOutputKind.URL_DECODED, decode(input)),
        )
    }

    fun encode(text: String): String = buildString {
        text.encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and 0xFF
            val char = code.toChar()

            if (code < 0x80 && char.isUnreserved()) {
                append(char)
            } else {
                append('%')
                append(HexDigits[code ushr 4])
                append(HexDigits[code and 0x0F])
            }
        }
    }

    fun decode(text: String): DevToolValue {
        val bytes = ArrayList<Byte>(text.length)
        var index = 0

        while (index < text.length) {
            val char = text[index]

            if (char == '%') {
                val high = text.getOrNull(index + 1)?.digitToIntOrNull(16)
                val low = text.getOrNull(index + 2)?.digitToIntOrNull(16)
                if (high == null || low == null) return DevToolValue.Error(DevToolError.InvalidPercentEncoding(index))

                bytes += ((high shl 4) or low).toByte()
                index += 3
            } else {
                // 인코드되지 않은 글자(한글 등)가 섞여 있어도 그 글자의 UTF-8 바이트로 옮긴다.
                val end = if (char.isHighSurrogate() && index + 1 < text.length) index + 2 else index + 1
                text.substring(index, end).encodeToByteArray().forEach { bytes += it }
                index = end
            }
        }

        return decodeUtf8(bytes.toByteArray())
    }

    private fun Char.isUnreserved(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_' || this == '.' || this == '~'
}

internal const val HexDigits = "0123456789ABCDEF"
