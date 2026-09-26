package io.github.taetae98coding.jarvis.ui.texttools

import io.github.taetae98coding.jarvis.domain.texttools.LimitBasis
import io.github.taetae98coding.jarvis.domain.texttools.PasswordStrength
import io.github.taetae98coding.jarvis.domain.texttools.TextStats
import io.github.taetae98coding.jarvis.domain.texttools.TextTool
import io.github.taetae98coding.jarvis.domain.texttools.TextTransform

internal val TextTool.label: String
    get() = when (this) {
        TextTool.COUNT -> "글자 수 세기"
        TextTool.PASSWORD -> "비밀번호 생성"
        TextTool.CASE -> "대소문자·공백"
    }

internal enum class StatKind(val label: String, val value: (TextStats) -> String) {
    WITH_SPACES("공백 포함", { it.charactersWithSpaces.grouped() + "자" }),
    WITHOUT_SPACES("공백 제외", { it.charactersWithoutSpaces.grouped() + "자" }),
    WORDS("단어", { it.words.grouped() + "개" }),
    LINES("줄", { it.lines.grouped() + "줄" }),
    PARAGRAPHS("문단", { it.paragraphs.grouped() + "개" }),
    UTF8_BYTES("바이트 (UTF-8)", { it.utf8Bytes.grouped() + "바이트" }),
    KOREAN_BYTES("바이트 (한글 2·영문 1)", { it.koreanBytes.grouped() + "바이트" }),
    READING_TIME("예상 읽기 시간", { readingTime(it.readingSeconds) }),
}

internal val LimitBasis.label: String
    get() = when (this) {
        LimitBasis.WITH_SPACES -> "공백 포함"
        LimitBasis.WITHOUT_SPACES -> "공백 제외"
        LimitBasis.KOREAN_BYTES -> "바이트"
    }

internal val LimitBasis.unit: String
    get() = if (this == LimitBasis.KOREAN_BYTES) "바이트" else "자"

internal val PasswordStrength.label: String
    get() = when (this) {
        PasswordStrength.WEAK -> "약함"
        PasswordStrength.FAIR -> "보통"
        PasswordStrength.STRONG -> "강함"
        PasswordStrength.VERY_STRONG -> "매우 강함"
    }

internal val TextTransform.label: String
    get() = when (this) {
        TextTransform.UPPERCASE -> "UPPER"
        TextTransform.LOWERCASE -> "lower"
        TextTransform.TITLE_CASE -> "Title Case"
        TextTransform.CAMEL_CASE -> "camelCase"
        TextTransform.SNAKE_CASE -> "snake_case"
        TextTransform.KEBAB_CASE -> "kebab-case"
        TextTransform.TRIM_WHITESPACE -> "공백 정리"
        TextTransform.REMOVE_LINE_BREAKS -> "줄바꿈 없애기"
    }

internal fun readingTime(seconds: Int): String =
    if (seconds < 60) "${seconds}초" else "${seconds / 60}분 ${seconds % 60}초"

/** 공통 코드에 로캘 숫자 서식이 없어 천 단위 쉼표를 직접 넣는다. */
internal fun Int.grouped(): String {
    val digits = toString().removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (this < 0) "-$grouped" else grouped
}
