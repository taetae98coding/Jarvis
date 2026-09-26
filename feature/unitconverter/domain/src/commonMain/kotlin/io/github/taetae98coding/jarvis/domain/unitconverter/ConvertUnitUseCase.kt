package io.github.taetae98coding.jarvis.domain.unitconverter

class ConvertUnitUseCase {
    operator fun invoke(input: String, from: MeasureUnit, to: MeasureUnit): UnitConversion = UnitConverter.convert(input, from, to)
}
