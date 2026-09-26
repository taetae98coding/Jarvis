package io.github.taetae98coding.jarvis.domain.devtools

/** 결과 한 줄. 이름은 화면이 [kind] 로 정한다. */
data class DevToolOutput(
    val kind: DevToolOutputKind,
    val value: DevToolValue,
)

sealed interface DevToolValue {
    data class Text(val text: String) : DevToolValue

    data class Error(val error: DevToolError) : DevToolValue
}

enum class DevToolOutputKind {
    TIMESTAMP_UNIT,
    ISO_8601,
    EPOCH_SECONDS,
    EPOCH_MILLISECONDS,
    BASE64_ENCODED,
    BASE64_DECODED,
    URL_ENCODED,
    URL_DECODED,
    JSON_PRETTY,
    JSON_MINIFIED,
    UUID,
    SHA256,
    SHA1,
    MD5,
    HEX,
    RGB,
    HSL,
}

sealed interface DevToolError {
    /** 정수도 ISO-8601 도 아니다. */
    data object InvalidTimestamp : DevToolError

    data object InvalidBase64 : DevToolError

    /** 디코드한 바이트가 UTF-8 이 아니다. */
    data object InvalidUtf8 : DevToolError

    /** [index] 의 `%` 뒤에 16진수 두 자리가 오지 않는다. */
    data class InvalidPercentEncoding(val index: Int) : DevToolError

    /** [line]·[column] 은 1부터 센다. */
    data class InvalidJson(val line: Int, val column: Int, val reason: JsonErrorReason) : DevToolError

    data object InvalidColor : DevToolError
}

enum class JsonErrorReason {
    UNEXPECTED_CHARACTER,
    UNEXPECTED_END,
    INVALID_ESCAPE,
    CONTROL_CHARACTER_IN_STRING,
    INVALID_NUMBER,
    TRAILING_CONTENT,
    TOO_DEEP,
}

/** 색 도구가 견본을 칠할 채널. 0–255. */
data class RgbColor(val red: Int, val green: Int, val blue: Int)
