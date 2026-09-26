package io.github.taetae98coding.jarvis.domain.unitconverter

enum class UnitCategory {
    LENGTH,
    AREA,
    MASS,
    VOLUME,
    TEMPERATURE,
    SPEED,
    DATA,
    TIME,
    ENERGY,
    ;

    /** 저장소에 적는 문자열. 서수 대신 이름을 써서 상수 순서를 바꿔도 저장값이 뒤바뀌지 않는다. */
    val storedValue: String get() = name.lowercase()

    /** [MeasureUnit] 선언 순서가 화면의 단위 순서다. */
    val units: List<MeasureUnit> get() = MeasureUnit.entries.filter { it.category == this }

    val defaultFrom: MeasureUnit
        get() = when (this) {
            LENGTH -> MeasureUnit.CENTIMETER
            AREA -> MeasureUnit.PYEONG
            MASS -> MeasureUnit.KILOGRAM
            VOLUME -> MeasureUnit.LITER
            TEMPERATURE -> MeasureUnit.CELSIUS
            SPEED -> MeasureUnit.KILOMETER_PER_HOUR
            DATA -> MeasureUnit.GIGABYTE
            TIME -> MeasureUnit.HOUR
            ENERGY -> MeasureUnit.KILOCALORIE
        }

    val defaultTo: MeasureUnit
        get() = when (this) {
            LENGTH -> MeasureUnit.INCH
            AREA -> MeasureUnit.SQUARE_METER
            MASS -> MeasureUnit.POUND
            VOLUME -> MeasureUnit.US_GALLON
            TEMPERATURE -> MeasureUnit.FAHRENHEIT
            SPEED -> MeasureUnit.MILE_PER_HOUR
            DATA -> MeasureUnit.GIBIBYTE
            TIME -> MeasureUnit.MINUTE
            ENERGY -> MeasureUnit.KILOJOULE
        }

    companion object {
        /** 모르는 값은 첫 분류다. 앱 밖에서 잘못 쓴 값 때문에 화면이 비지 않게 한다. */
        fun fromStored(value: String?): UnitCategory = entries.firstOrNull { it.storedValue == value } ?: LENGTH
    }
}
