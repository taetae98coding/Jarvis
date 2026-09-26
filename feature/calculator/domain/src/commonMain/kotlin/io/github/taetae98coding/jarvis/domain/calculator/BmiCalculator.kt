package io.github.taetae98coding.jarvis.domain.calculator

import kotlin.math.floor

/** 대한비만학회 성인 기준 단계. */
enum class BmiCategory {
    UNDERWEIGHT,
    NORMAL,
    PRE_OBESE,
    OBESE_1,
    OBESE_2,
    OBESE_3,
}

sealed interface BmiResult {
    data object Empty : BmiResult

    data object InvalidHeight : BmiResult

    data object InvalidWeight : BmiResult

    /** 글자는 모두 소수 첫째 자리까지 적는다. 몸무게 범위는 이 키에서 정상(18.5–22.9)에 드는 kg 이다. */
    data class Value(
        val bmi: Double,
        val text: String,
        val category: BmiCategory,
        val normalWeightMinText: String,
        val normalWeightMaxText: String,
    ) : BmiResult
}

object BmiCalculator {
    val HeightRangeCm: ClosedFloatingPointRange<Double> = 30.0..300.0
    val WeightRangeKg: ClosedFloatingPointRange<Double> = 1.0..500.0

    // 기준표의 경계를 소수 첫째 자리 정수로 둔다. 반올림한 BMI 와 정수끼리 비교해 화면 값과 단계가 어긋나지 않게 한다.
    private const val NormalMinTenths = 185L
    private const val NormalMaxTenths = 229L
    private const val PreObeseTenths = 230L
    private const val Obese1Tenths = 250L
    private const val Obese2Tenths = 300L
    private const val Obese3Tenths = 350L

    fun calculate(heightCm: String, weightKg: String): BmiResult {
        if (heightCm.isBlank() || weightKg.isBlank()) return BmiResult.Empty

        val height = parseSignedDecimal(heightCm)?.takeIf { it in HeightRangeCm } ?: return BmiResult.InvalidHeight
        val weight = parseSignedDecimal(weightKg)?.takeIf { it in WeightRangeKg } ?: return BmiResult.InvalidWeight

        val meters = height / 100
        val squared = meters * meters
        val bmi = weight / squared
        val tenths = roundToTenths(bmi)

        return BmiResult.Value(
            bmi = bmi,
            text = formatTenths(tenths),
            category = categoryOf(tenths),
            normalWeightMinText = formatTenths(roundToTenths(NormalMinTenths / 10.0 * squared)),
            normalWeightMaxText = formatTenths(roundToTenths(NormalMaxTenths / 10.0 * squared)),
        )
    }

    fun categoryOf(tenths: Long): BmiCategory =
        when {
            tenths < NormalMinTenths -> BmiCategory.UNDERWEIGHT
            tenths < PreObeseTenths -> BmiCategory.NORMAL
            tenths < Obese1Tenths -> BmiCategory.PRE_OBESE
            tenths < Obese2Tenths -> BmiCategory.OBESE_1
            tenths < Obese3Tenths -> BmiCategory.OBESE_2
            else -> BmiCategory.OBESE_3
        }

    /** 반올림은 0.5 에서 올린다. `kotlin.math.round` 는 짝수 쪽으로 반올림해 22.25 가 22.2 가 된다. */
    private fun roundToTenths(value: Double): Long = floor(value * 10 + 0.5).toLong()

    private fun formatTenths(tenths: Long): String = "${tenths / 10}.${tenths % 10}"
}
