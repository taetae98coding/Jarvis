package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * 입력 숫자를 읽고 결과를 글자로 쓴다. 규칙은 docs/common/unit-converter.html#number-format 이다.
 *
 * `String.format` 과 `BigDecimal` 이 공통 stdlib 에 없어서 자릿수 반올림을 Long 으로 직접 한다.
 */
object UnitNumberFormat {
    const val SignificantDigits = 12

    // 이 범위 밖은 지수 표기로 쓴다. 위쪽은 Long 반올림이 Double 의 정확한 정수 범위(2^53)를 넘지 않게 하는 한계이기도 하다.
    private const val PlainMin = 1e-6
    private const val PlainMax = 1e15

    // toDoubleOrNull 은 JVM 에서 16진 부동소수(0x1p3)와 형 접미사(1f, 1d)까지 받는다. 네 타깃이 같은 입력을 받도록 문법을 먼저 좁힌다.
    private val NumberPattern = Regex("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")

    /** 앞뒤 공백과 자릿수 구분 쉼표(`1,234.5`)는 무시한다. 숫자가 아니거나 유한하지 않으면 null 이다. */
    fun parse(input: String): Double? {
        val text = input.trim().replace(",", "")
        if (!NumberPattern.matches(text)) return null
        return text.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /** 유효 숫자 [SignificantDigits] 자리로 반올림하고 끝의 0 을 지운다. 유한하지 않으면 null 이다. */
    fun format(value: Double): String? {
        if (!value.isFinite()) return null
        if (value == 0.0) return "0"

        val magnitude = abs(value)
        val sign = if (value < 0) "-" else ""
        val exponent = floor(log10(magnitude)).toInt()

        return if (magnitude >= PlainMin && magnitude < PlainMax) {
            // 정수 부분은 자르지 않는다. 1 TiB = 1099511627776 B 처럼 12자리를 넘는 정확한 정수가 뭉개지지 않게 한다.
            val decimals = (SignificantDigits - 1 - exponent).coerceAtLeast(0)
            sign + fixed(magnitude, decimals)
        } else {
            scientific(magnitude, exponent, sign)
        }
    }

    private fun scientific(magnitude: Double, estimate: Int, sign: String): String {
        // log10 이 경계에서 한 자리 어긋날 수 있어 가수가 [1, 10) 에 들도록 고친다.
        var exponent = estimate
        var mantissa = magnitude / 10.0.pow(exponent)
        if (mantissa >= 10) {
            exponent++
            mantissa /= 10
        } else if (mantissa < 1) {
            exponent--
            mantissa *= 10
        }

        var digits = fixed(mantissa, SignificantDigits - 1)
        // 9.9999999999995 처럼 반올림이 자리를 올리면 10 이 된다.
        if (digits.startsWith("10")) {
            exponent++
            digits = fixed(mantissa / 10, SignificantDigits - 1)
        }
        return "$sign${digits}E$exponent"
    }

    /** 0 이상인 [value] 를 소수 [decimals] 자리에서 반올림(0.5 는 올림)한 글자. 끝의 0 과 남은 점은 지운다. */
    private fun fixed(value: Double, decimals: Int): String {
        val scaled = floor(value * 10.0.pow(decimals) + 0.5).toLong().toString()
        if (decimals == 0) return scaled

        val padded = scaled.padStart(decimals + 1, '0')
        val integer = padded.dropLast(decimals)
        val fraction = padded.takeLast(decimals).trimEnd('0')
        return if (fraction.isEmpty()) integer else "$integer.$fraction"
    }
}
