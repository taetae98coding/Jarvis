package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 주소 입력칸에 친 것을 열 주소로 바꾼다. 빈 입력은 null 이다.
 * 규칙은 docs/common/terminal-browser.html R4.
 */
fun browserAddress(input: String): String? {
    val text = input.trim()
    if (text.isEmpty()) return null

    val oneWord = text.none { it.isWhitespace() }
    return when {
        "://" in text -> text
        oneWord && text.startsWith("localhost") -> "http://$text"
        oneWord && '.' in text -> "https://$text"
        else -> "$SearchUrl${percentEncode(text)}"
    }
}

private const val SearchUrl = "https://www.google.com/search?q="

// 공통 코드에는 URLEncoder 가 없다. 쿼리 값에 그대로 둬도 되는 글자(RFC 3986 unreserved)만 남긴다.
private fun percentEncode(text: String): String =
    text.encodeToByteArray().joinToString("") { byte ->
        val char = byte.toInt().toChar()
        if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in "-._~") {
            char.toString()
        } else {
            "%" + (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
    }
