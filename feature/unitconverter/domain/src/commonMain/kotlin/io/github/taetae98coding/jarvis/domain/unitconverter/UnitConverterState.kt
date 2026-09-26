package io.github.taetae98coding.jarvis.domain.unitconverter

/** 한 분류에서 마지막에 고른 두 단위와 입력. 분류마다 따로 남는다. */
data class UnitSelection(
    val from: MeasureUnit,
    val to: MeasureUnit,
    val input: String,
) {
    companion object {
        fun default(category: UnitCategory): UnitSelection = UnitSelection(category.defaultFrom, category.defaultTo, "")
    }
}

data class UnitConverterState(
    val category: UnitCategory,
    val selection: UnitSelection,
)
