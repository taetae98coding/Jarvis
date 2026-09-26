package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * 답을 부동소수점 잡음 없이 적는다(docs/common/calculator.html R6).
 *
 * `Double.toString()` 은 타깃마다 글자가 달라(JVM `1.0E10`, JS·Wasm `10000000000`) 쓰지 않는다.
 */
object CalculatorNumberFormat {
    const val SignificantDigits = 12

    // 지수 없이 적는 10진 지수 범위. 10^15 부터는 Long 으로 옮겨도 Double 의 정밀도(약 15.9자리)를 넘는다.
    private const val MaxPlainExponent = 14
    private const val MinPlainExponent = -9
    private const val PlainIntegerLimit = 1e15

    private val MantissaLimit = 10.0.pow(SignificantDigits).toLong()

    fun format(value: Double): String {
        if (!value.isFinite()) return value.toString()
        if (value == 0.0) return "0"

        val magnitude = abs(value)
        val body = if (magnitude < PlainIntegerLimit && magnitude == floor(magnitude)) {
            magnitude.toLong().toString()
        } else {
            val (digits, exponent) = significantDigits(magnitude)
            if (exponent in MinPlainExponent..MaxPlainExponent) plain(digits, exponent) else scientific(digits, exponent)
        }
        return if (value < 0) "-$body" else body
    }

    /** [SignificantDigits] 자리로 반올림한 숫자열(첫 자리가 0 이 아님)과 첫 자리의 10진 지수. */
    private fun significantDigits(magnitude: Double): Pair<String, Int> {
        var exponent = floor(log10(magnitude)).toInt()
        // log10 은 10 의 거듭제곱 근처에서 한 자리 어긋날 수 있다(log10(1000.0) = 2.9999…). 반올림한 자릿수로 바로잡는다.
        repeat(2) {
            val mantissa = floor(scale(magnitude, SignificantDigits - 1 - exponent) + 0.5).toLong()
            when {
                mantissa >= MantissaLimit -> exponent++
                mantissa < MantissaLimit / 10 -> exponent--
                else -> return mantissa.toString() to exponent
            }
        }
        val mantissa = floor(scale(magnitude, SignificantDigits - 1 - exponent) + 0.5).toLong().coerceIn(MantissaLimit / 10, MantissaLimit - 1)
        return mantissa.toString() to exponent
    }

    // 10^shift 한 번으로 곱하면 아주 작은 수(1e-300)에서 10^311 이 무한대가 된다. 둘로 나눠 곱한다.
    // 음수 shift 는 나눗셈으로 한다. 10^-n 은 2진수로 정확하지 않지만 10^n 은 n ≤ 22 까지 정확하다.
    private fun scale(magnitude: Double, shift: Int): Double =
        if (shift >= 0) {
            val half = shift / 2
            magnitude * 10.0.pow(half) * 10.0.pow(shift - half)
        } else {
            magnitude / 10.0.pow(-shift)
        }

    private fun plain(digits: String, exponent: Int): String {
        val integer: String
        val fraction: String
        if (exponent >= 0) {
            val integerLength = exponent + 1
            integer = if (integerLength <= digits.length) digits.substring(0, integerLength) else digits.padEnd(integerLength, '0')
            fraction = digits.substring(minOf(integerLength, digits.length))
        } else {
            integer = "0"
            fraction = "0".repeat(-exponent - 1) + digits
        }
        val trimmed = fraction.trimEnd('0')
        return if (trimmed.isEmpty()) integer else "$integer.$trimmed"
    }

    private fun scientific(digits: String, exponent: Int): String {
        val fraction = digits.substring(1).trimEnd('0')
        val mantissa = if (fraction.isEmpty()) digits.substring(0, 1) else "${digits[0]}.$fraction"
        return "${mantissa}e$exponent"
    }
}
