package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnitConverterTest {
    private fun text(input: String, from: MeasureUnit, to: MeasureUnit): String? =
        assertIs<UnitConversion.Converted>(UnitConverter.convert(input, from, to)).result.text

    @Test
    fun lengthKnownValues() {
        assertEquals("1.609344", text("1", MeasureUnit.MILE, MeasureUnit.KILOMETER))
        assertEquals("2.54", text("1", MeasureUnit.INCH, MeasureUnit.CENTIMETER))
        assertEquals("12", text("1", MeasureUnit.FOOT, MeasureUnit.INCH))
        assertEquals("3", text("1", MeasureUnit.YARD, MeasureUnit.FOOT))
        assertEquals("1852", text("1", MeasureUnit.NAUTICAL_MILE, MeasureUnit.METER))
        assertEquals("30.303030303", text("1", MeasureUnit.JA, MeasureUnit.CENTIMETER))
        assertEquals("3.3", text("1", MeasureUnit.METER, MeasureUnit.JA))
        assertEquals("1000000", text("1", MeasureUnit.KILOMETER, MeasureUnit.MILLIMETER))
    }

    @Test
    fun areaKnownValues() {
        assertEquals("3.30578512397", text("1", MeasureUnit.PYEONG, MeasureUnit.SQUARE_METER))
        assertEquals("0.3025", text("1", MeasureUnit.SQUARE_METER, MeasureUnit.PYEONG))
        // 전용 84 m² 는 흔히 "25.4평" 으로 부른다.
        assertEquals("25.41", text("84", MeasureUnit.SQUARE_METER, MeasureUnit.PYEONG))
        assertEquals("3025", text("1", MeasureUnit.HECTARE, MeasureUnit.PYEONG))
        assertEquals("43560", text("1", MeasureUnit.ACRE, MeasureUnit.SQUARE_FOOT))
        assertEquals("4046.8564224", text("1", MeasureUnit.ACRE, MeasureUnit.SQUARE_METER))
        assertEquals("640", text("1", MeasureUnit.SQUARE_MILE, MeasureUnit.ACRE))
        assertEquals("100", text("1", MeasureUnit.SQUARE_KILOMETER, MeasureUnit.HECTARE))
    }

    @Test
    fun massKnownValues() {
        assertEquals("0.45359237", text("1", MeasureUnit.POUND, MeasureUnit.KILOGRAM))
        assertEquals("16", text("1", MeasureUnit.POUND, MeasureUnit.OUNCE))
        assertEquals("28.349523125", text("1", MeasureUnit.OUNCE, MeasureUnit.GRAM))
        assertEquals("3.75", text("1", MeasureUnit.DON, MeasureUnit.GRAM))
        assertEquals("10", text("1", MeasureUnit.NYANG, MeasureUnit.DON))
        assertEquals("160", text("1", MeasureUnit.GEUN, MeasureUnit.DON))
        assertEquals("600", text("1", MeasureUnit.GEUN, MeasureUnit.GRAM))
        assertEquals("1000000000", text("1", MeasureUnit.TONNE, MeasureUnit.MILLIGRAM))
    }

    @Test
    fun volumeKnownValues() {
        assertEquals("3.785411784", text("1", MeasureUnit.US_GALLON, MeasureUnit.LITER))
        assertEquals("16", text("1", MeasureUnit.US_CUP, MeasureUnit.US_TABLESPOON))
        assertEquals("3", text("1", MeasureUnit.US_TABLESPOON, MeasureUnit.US_TEASPOON))
        assertEquals("128", text("1", MeasureUnit.US_GALLON, MeasureUnit.US_FLUID_OUNCE))
        assertEquals("236.5882365", text("1", MeasureUnit.US_CUP, MeasureUnit.MILLILITER))
        assertEquals("1.80390683696", text("1", MeasureUnit.DOE, MeasureUnit.LITER))
        assertEquals("1000", text("1", MeasureUnit.CUBIC_METER, MeasureUnit.LITER))
    }

    @Test
    fun temperatureIsAffine() {
        assertEquals("212", text("100", MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT))
        assertEquals("32", text("0", MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT))
        assertEquals("0", text("32", MeasureUnit.FAHRENHEIT, MeasureUnit.CELSIUS))
        assertEquals("-40", text("-40", MeasureUnit.CELSIUS, MeasureUnit.FAHRENHEIT))
        assertEquals("-40", text("-40", MeasureUnit.FAHRENHEIT, MeasureUnit.CELSIUS))
        assertEquals("273.15", text("0", MeasureUnit.CELSIUS, MeasureUnit.KELVIN))
        assertEquals("-273.15", text("0", MeasureUnit.KELVIN, MeasureUnit.CELSIUS))
        assertEquals("-459.67", text("0", MeasureUnit.KELVIN, MeasureUnit.FAHRENHEIT))
        assertEquals("37", text("98.6", MeasureUnit.FAHRENHEIT, MeasureUnit.CELSIUS))
    }

    @Test
    fun speedKnownValues() {
        assertEquals("27.7777777778", text("100", MeasureUnit.KILOMETER_PER_HOUR, MeasureUnit.METER_PER_SECOND))
        assertEquals("1.852", text("1", MeasureUnit.KNOT, MeasureUnit.KILOMETER_PER_HOUR))
        assertEquals("96.56064", text("60", MeasureUnit.MILE_PER_HOUR, MeasureUnit.KILOMETER_PER_HOUR))
        assertEquals("3.6", text("1", MeasureUnit.METER_PER_SECOND, MeasureUnit.KILOMETER_PER_HOUR))
    }

    @Test
    fun dataSeparatesDecimalAndBinaryPrefixes() {
        assertEquals("1073741824", text("1", MeasureUnit.GIBIBYTE, MeasureUnit.BYTE))
        assertEquals("1099511627776", text("1", MeasureUnit.TEBIBYTE, MeasureUnit.BYTE))
        assertEquals("1000000000", text("1", MeasureUnit.GIGABYTE, MeasureUnit.BYTE))
        assertEquals("1024", text("1", MeasureUnit.KIBIBYTE, MeasureUnit.BYTE))
        assertEquals("1000", text("1", MeasureUnit.KILOBYTE, MeasureUnit.BYTE))
        assertEquals("8", text("1", MeasureUnit.BYTE, MeasureUnit.BIT))
        // "1 TB 디스크가 931 GiB 로 보이는" 차이.
        assertEquals("931.322574615", text("1", MeasureUnit.TERABYTE, MeasureUnit.GIBIBYTE))
        assertEquals("1024", text("1", MeasureUnit.TEBIBYTE, MeasureUnit.GIBIBYTE))
    }

    @Test
    fun timeAndEnergyKnownValues() {
        assertEquals("168", text("1", MeasureUnit.WEEK, MeasureUnit.HOUR))
        assertEquals("86400", text("1", MeasureUnit.DAY, MeasureUnit.SECOND))
        assertEquals("1.5", text("1500", MeasureUnit.MILLISECOND, MeasureUnit.SECOND))
        assertEquals("90", text("1.5", MeasureUnit.HOUR, MeasureUnit.MINUTE))
        assertEquals("4.184", text("1", MeasureUnit.KILOCALORIE, MeasureUnit.KILOJOULE))
        assertEquals("3600", text("1", MeasureUnit.KILOWATT_HOUR, MeasureUnit.KILOJOULE))
        assertEquals("860.420650096", text("1", MeasureUnit.KILOWATT_HOUR, MeasureUnit.KILOCALORIE))
        assertEquals("1000", text("1", MeasureUnit.KILOCALORIE, MeasureUnit.CALORIE))
    }

    @Test
    fun everyPairRoundTrips() {
        val samples = listOf(1.0, 123.456, -40.0, 0.001, 98_765.4321)
        UnitCategory.entries.forEach { category ->
            category.units.forEach { from ->
                category.units.forEach { to ->
                    samples.forEach { value ->
                        val back = UnitConverter.convert(UnitConverter.convert(value, from, to), to, from)
                        assertTrue(abs(back - value) <= 1e-9 * max(1.0, abs(value)), "$value $from → $to → $from = $back")
                    }
                }
            }
        }
    }

    @Test
    fun sameUnitIsIdentity() {
        assertEquals(0.1 + 0.2, UnitConverter.convert(0.1 + 0.2, MeasureUnit.FAHRENHEIT, MeasureUnit.FAHRENHEIT))
    }

    @Test
    fun resultListsEveryUnitOfTheCategoryInOrder() {
        val converted = assertIs<UnitConversion.Converted>(UnitConverter.convert("1", MeasureUnit.KILOMETER, MeasureUnit.MILE))

        assertEquals(UnitCategory.LENGTH.units, converted.all.map { it.unit })
        assertEquals(MeasureUnit.MILE, converted.result.unit)
        assertEquals("1000", converted.all.first { it.unit == MeasureUnit.METER }.text)
        assertEquals("1", converted.all.first { it.unit == MeasureUnit.KILOMETER }.text)
        assertEquals(1.0, converted.value)
    }

    @Test
    fun blankAndInvalidInput() {
        assertEquals(UnitConversion.Empty, UnitConverter.convert("", MeasureUnit.METER, MeasureUnit.FOOT))
        assertEquals(UnitConversion.Empty, UnitConverter.convert("   ", MeasureUnit.METER, MeasureUnit.FOOT))
        assertEquals(UnitConversion.InvalidNumber, UnitConverter.convert("1m", MeasureUnit.METER, MeasureUnit.FOOT))
        assertEquals(UnitConversion.InvalidNumber, UnitConverter.convert("-", MeasureUnit.METER, MeasureUnit.FOOT))
    }

    @Test
    fun overflowingUnitHasNoText() {
        val converted = assertIs<UnitConversion.Converted>(UnitConverter.convert("1e308", MeasureUnit.KILOMETER, MeasureUnit.METER))

        assertNull(converted.all.first { it.unit == MeasureUnit.MILLIMETER }.text)
        assertEquals("1E308", converted.all.first { it.unit == MeasureUnit.KILOMETER }.text)
    }

    @Test
    fun crossCategoryIsRejected() {
        assertFailsWith<IllegalArgumentException> { UnitConverter.convert(1.0, MeasureUnit.METER, MeasureUnit.GRAM) }
    }

    @Test
    fun everyCategoryHasItsDefaultsAndStoredValuesAreUnique() {
        UnitCategory.entries.forEach { category ->
            assertTrue(category.defaultFrom in category.units, "$category")
            assertTrue(category.defaultTo in category.units, "$category")
            assertTrue(category.defaultFrom != category.defaultTo, "$category")
        }
        assertEquals(MeasureUnit.entries.size, MeasureUnit.entries.map { it.storedValue }.toSet().size)
    }

    @Test
    fun storedUnitMustBelongToCategory() {
        assertEquals(MeasureUnit.PYEONG, MeasureUnit.fromStored(UnitCategory.AREA, "pyeong"))
        assertNull(MeasureUnit.fromStored(UnitCategory.LENGTH, "pyeong"))
        assertNull(MeasureUnit.fromStored(UnitCategory.LENGTH, null))
        assertEquals(UnitCategory.LENGTH, UnitCategory.fromStored("removed"))
        assertEquals(UnitCategory.DATA, UnitCategory.fromStored("data"))
    }
}
