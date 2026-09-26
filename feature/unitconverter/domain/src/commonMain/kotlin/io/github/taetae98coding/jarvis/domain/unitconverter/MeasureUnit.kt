package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlin.math.abs

/**
 * 분류마다 기준 단위(m, m², kg, L, K, m/s, bit, s, J)가 있고, 단위는 `기준값 = (값 + shift) × factor` 로 그 기준에 닿는다.
 * shift 는 온도만 쓴다.
 *
 * 값의 출처와 정의는 docs/common/unit-converter.html#definitions 에 있다. 국제 야드·파운드(1959), 미국 관습 부피, 계량법의 척관법 환산값이다.
 */
enum class MeasureUnit(
    val category: UnitCategory,
    private val factor: Double,
    private val shift: Double = 0.0,
) {
    MILLIMETER(UnitCategory.LENGTH, 0.001),
    CENTIMETER(UnitCategory.LENGTH, 0.01),
    METER(UnitCategory.LENGTH, 1.0),
    KILOMETER(UnitCategory.LENGTH, 1000.0),
    INCH(UnitCategory.LENGTH, 0.0254),
    FOOT(UnitCategory.LENGTH, 0.3048),
    YARD(UnitCategory.LENGTH, 0.9144),
    MILE(UnitCategory.LENGTH, 1609.344),
    NAUTICAL_MILE(UnitCategory.LENGTH, 1852.0),
    JA(UnitCategory.LENGTH, 10.0 / 33.0),

    SQUARE_CENTIMETER(UnitCategory.AREA, 0.0001),
    SQUARE_METER(UnitCategory.AREA, 1.0),
    PYEONG(UnitCategory.AREA, 400.0 / 121.0),
    HECTARE(UnitCategory.AREA, 10_000.0),
    SQUARE_KILOMETER(UnitCategory.AREA, 1_000_000.0),
    SQUARE_FOOT(UnitCategory.AREA, 0.09290304),
    SQUARE_YARD(UnitCategory.AREA, 0.83612736),
    ACRE(UnitCategory.AREA, 4046.8564224),
    SQUARE_MILE(UnitCategory.AREA, 2_589_988.110336),

    MILLIGRAM(UnitCategory.MASS, 0.000001),
    GRAM(UnitCategory.MASS, 0.001),
    KILOGRAM(UnitCategory.MASS, 1.0),
    TONNE(UnitCategory.MASS, 1000.0),
    OUNCE(UnitCategory.MASS, 0.028349523125),
    POUND(UnitCategory.MASS, 0.45359237),
    DON(UnitCategory.MASS, 0.00375),
    NYANG(UnitCategory.MASS, 0.0375),
    GEUN(UnitCategory.MASS, 0.6),

    MILLILITER(UnitCategory.VOLUME, 0.001),
    LITER(UnitCategory.VOLUME, 1.0),
    CUBIC_METER(UnitCategory.VOLUME, 1000.0),
    US_TEASPOON(UnitCategory.VOLUME, 0.00492892159375),
    US_TABLESPOON(UnitCategory.VOLUME, 0.01478676478125),
    US_FLUID_OUNCE(UnitCategory.VOLUME, 0.0295735295625),
    US_CUP(UnitCategory.VOLUME, 0.2365882365),
    US_GALLON(UnitCategory.VOLUME, 3.785411784),
    DOE(UnitCategory.VOLUME, 2401.0 / 1331.0),

    CELSIUS(UnitCategory.TEMPERATURE, 1.0, shift = 273.15),
    FAHRENHEIT(UnitCategory.TEMPERATURE, 5.0 / 9.0, shift = 459.67),
    KELVIN(UnitCategory.TEMPERATURE, 1.0),

    METER_PER_SECOND(UnitCategory.SPEED, 1.0),
    KILOMETER_PER_HOUR(UnitCategory.SPEED, 1000.0 / 3600.0),
    MILE_PER_HOUR(UnitCategory.SPEED, 1609.344 / 3600.0),
    KNOT(UnitCategory.SPEED, 1852.0 / 3600.0),

    BIT(UnitCategory.DATA, 1.0),
    BYTE(UnitCategory.DATA, 8.0),
    KILOBYTE(UnitCategory.DATA, 8.0e3),
    MEGABYTE(UnitCategory.DATA, 8.0e6),
    GIGABYTE(UnitCategory.DATA, 8.0e9),
    TERABYTE(UnitCategory.DATA, 8.0e12),
    KIBIBYTE(UnitCategory.DATA, 8.0 * 1024),
    MEBIBYTE(UnitCategory.DATA, 8.0 * 1024 * 1024),
    GIBIBYTE(UnitCategory.DATA, 8.0 * 1024 * 1024 * 1024),
    TEBIBYTE(UnitCategory.DATA, 8.0 * 1024 * 1024 * 1024 * 1024),

    MILLISECOND(UnitCategory.TIME, 0.001),
    SECOND(UnitCategory.TIME, 1.0),
    MINUTE(UnitCategory.TIME, 60.0),
    HOUR(UnitCategory.TIME, 3600.0),
    DAY(UnitCategory.TIME, 86_400.0),
    WEEK(UnitCategory.TIME, 604_800.0),

    JOULE(UnitCategory.ENERGY, 1.0),
    KILOJOULE(UnitCategory.ENERGY, 1000.0),
    CALORIE(UnitCategory.ENERGY, 4.184),
    KILOCALORIE(UnitCategory.ENERGY, 4184.0),
    WATT_HOUR(UnitCategory.ENERGY, 3600.0),
    KILOWATT_HOUR(UnitCategory.ENERGY, 3_600_000.0),
    ;

    val storedValue: String get() = name.lowercase()

    fun toBase(value: Double): Double = (value + shift) * factor

    fun fromBase(base: Double): Double {
        val value = base / factor - shift
        // 32 °F → °C 는 491.67 × 5/9 − 273.15 라서 0 대신 5.7E-14 가 남는다. 빼기 전 크기에 비해 이만큼 작은 값은
        // 반올림 찌꺼기이므로 0 으로 본다. 곱셈만 하는 단위는 상대 오차만 생겨 유효 숫자 반올림이 지운다.
        return if (shift != 0.0 && abs(value) < abs(shift) * ShiftNoise) 0.0 else value
    }

    companion object {
        private const val ShiftNoise = 1e-12

        /** [category] 에 없는 단위나 모르는 값이면 null 이다. 저장값이 다른 분류의 단위를 가리키는 경우도 걸러낸다. */
        fun fromStored(category: UnitCategory, value: String?): MeasureUnit? =
            entries.firstOrNull { it.category == category && it.storedValue == value }
    }
}
