package io.github.taetae98coding.jarvis.domain.unitconverter

sealed interface UnitConversion {
    data object Empty : UnitConversion

    data object InvalidNumber : UnitConversion

    /** [all] 은 분류의 모든 단위를 [UnitCategory.units] 순서로 담는다. [result] 도 그 안에 있다. */
    data class Converted(
        val value: Double,
        val result: UnitAmount,
        val all: List<UnitAmount>,
    ) : UnitConversion
}

/** [text] 는 [UnitNumberFormat.format] 결과이고, 값이 Double 범위를 넘으면 null 이다. */
data class UnitAmount(
    val unit: MeasureUnit,
    val value: Double,
    val text: String?,
)

object UnitConverter {
    fun convert(value: Double, from: MeasureUnit, to: MeasureUnit): Double {
        require(from.category == to.category) { "$from 과 $to 는 분류가 다르다" }
        return if (from == to) value else to.fromBase(from.toBase(value))
    }

    fun convert(input: String, from: MeasureUnit, to: MeasureUnit): UnitConversion {
        if (input.isBlank()) return UnitConversion.Empty
        val value = UnitNumberFormat.parse(input) ?: return UnitConversion.InvalidNumber

        val all = from.category.units.map { unit ->
            val converted = convert(value, from, unit)
            UnitAmount(unit, converted, UnitNumberFormat.format(converted))
        }
        return UnitConversion.Converted(value = value, result = all.first { it.unit == to }, all = all)
    }
}
