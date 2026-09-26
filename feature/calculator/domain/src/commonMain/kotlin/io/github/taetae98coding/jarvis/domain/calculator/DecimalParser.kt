package io.github.taetae98coding.jarvis.domain.calculator

internal fun Char.isDecimalDigit(): Boolean = this in '0'..'9'

/**
 * 부호 없는 10진수(`12`, `1.5`, `.5`, `5.`)를 읽는다. 숫자가 없거나 점이 둘이면 null.
 *
 * `String.toDouble()` 에 넘기기 전에 `0.5`·`5` 꼴로 고친다. `.5`·`5.` 을 받는지가 타깃의 파서마다 달라서다.
 */
internal fun parseDecimal(text: String): Double? {
    if (text.isEmpty() || text.count { it == '.' } > 1 || text.any { !it.isDecimalDigit() && it != '.' }) return null

    val integer = text.substringBefore('.')
    val fraction = text.substringAfter('.', missingDelimiterValue = "")
    if (integer.isEmpty() && fraction.isEmpty()) return null

    val normalized = integer.ifEmpty { "0" } + if (fraction.isEmpty()) "" else ".$fraction"
    return normalized.toDouble()
}

/** 입력 칸 하나에 든 숫자. 앞뒤 공백과 부호(`-`·`−`·`+`) 하나를 받고, 쉼표는 받지 않는다. */
internal fun parseSignedDecimal(text: String): Double? {
    val trimmed = text.trim()
    val sign = trimmed.firstOrNull()
    return when (sign) {
        '-', '−' -> parseDecimal(trimmed.substring(1))?.let { -it }
        '+' -> parseDecimal(trimmed.substring(1))
        else -> parseDecimal(trimmed)
    }
}
