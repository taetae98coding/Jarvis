package io.github.taetae98coding.jarvis.ui.devtools

import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import io.github.taetae98coding.jarvis.domain.devtools.DevToolError
import io.github.taetae98coding.jarvis.domain.devtools.DevToolOutputKind
import io.github.taetae98coding.jarvis.domain.devtools.JsonErrorReason
import io.github.taetae98coding.jarvis.domain.devtools.JsonFormatter
import io.github.taetae98coding.jarvis.domain.devtools.TimestampUnit

internal val DevTool.label: String
    get() = when (this) {
        DevTool.TIMESTAMP -> "타임스탬프"
        DevTool.BASE64 -> "Base64"
        DevTool.URL -> "URL"
        DevTool.JSON -> "JSON"
        DevTool.UUID -> "UUID"
        DevTool.HASH -> "해시"
        DevTool.COLOR -> "색"
    }

internal val DevTool.placeholder: String
    get() = when (this) {
        DevTool.TIMESTAMP -> "1700000000 또는 2023-11-14T22:13:20Z"
        DevTool.BASE64 -> "인코드하거나 디코드할 글자"
        DevTool.URL -> "https://example.com/?q=값"
        DevTool.JSON -> "{\"a\": 1}"
        DevTool.UUID -> ""
        DevTool.HASH -> "해시할 글자"
        DevTool.COLOR -> "#2E5DAA, rgb(46, 93, 170), hsl(217, 57%, 42%)"
    }

internal val DevToolOutputKind.label: String
    get() = when (this) {
        DevToolOutputKind.TIMESTAMP_UNIT -> "판별한 단위"
        DevToolOutputKind.ISO_8601 -> "ISO-8601 (UTC)"
        DevToolOutputKind.EPOCH_SECONDS -> "Unix 초"
        DevToolOutputKind.EPOCH_MILLISECONDS -> "Unix 밀리초"
        DevToolOutputKind.BASE64_ENCODED, DevToolOutputKind.URL_ENCODED -> "인코드"
        DevToolOutputKind.BASE64_DECODED, DevToolOutputKind.URL_DECODED -> "디코드"
        DevToolOutputKind.JSON_PRETTY -> "정렬"
        DevToolOutputKind.JSON_MINIFIED -> "한 줄"
        DevToolOutputKind.UUID -> "UUID"
        DevToolOutputKind.SHA256 -> "SHA-256"
        DevToolOutputKind.SHA1 -> "SHA-1"
        DevToolOutputKind.MD5 -> "MD5"
        DevToolOutputKind.HEX -> "HEX"
        DevToolOutputKind.RGB -> "RGB"
        DevToolOutputKind.HSL -> "HSL"
    }

/** 판별한 단위 줄은 도메인이 [TimestampUnit] 이름을 값으로 준다. 나머지는 그대로다. */
internal fun displayValue(kind: DevToolOutputKind, text: String): String =
    if (kind == DevToolOutputKind.TIMESTAMP_UNIT) {
        when (text) {
            TimestampUnit.SECONDS.name -> "초"
            TimestampUnit.MILLISECONDS.name -> "밀리초"
            else -> text
        }
    } else {
        text
    }

internal val DevToolError.message: String
    get() = when (this) {
        DevToolError.InvalidTimestamp -> "정수 타임스탬프나 ISO-8601 날짜가 아닙니다"
        DevToolError.InvalidBase64 -> "Base64 가 아닙니다"
        DevToolError.InvalidUtf8 -> "UTF-8 글자가 아닙니다"
        is DevToolError.InvalidPercentEncoding -> "${index + 1}번째 글자 % 뒤에 16진수 두 자리가 없습니다"
        is DevToolError.InvalidJson -> "$line 줄 $column 칸: ${reason.message}"
        DevToolError.InvalidColor -> "#RGB, #RRGGBB, rgb(r, g, b), hsl(h, s%, l%) 중 하나가 아닙니다"
    }

private val JsonErrorReason.message: String
    get() = when (this) {
        JsonErrorReason.UNEXPECTED_CHARACTER -> "예상하지 못한 글자"
        JsonErrorReason.UNEXPECTED_END -> "입력이 중간에 끝남"
        JsonErrorReason.INVALID_ESCAPE -> "잘못된 이스케이프"
        JsonErrorReason.CONTROL_CHARACTER_IN_STRING -> "문자열 안의 제어 문자(줄바꿈은 \\n 으로 적는다)"
        JsonErrorReason.INVALID_NUMBER -> "잘못된 숫자"
        JsonErrorReason.TRAILING_CONTENT -> "값 뒤에 남은 글자"
        JsonErrorReason.TOO_DEEP -> "중첩이 ${JsonFormatter.MaxDepth} 단계를 넘음"
    }
