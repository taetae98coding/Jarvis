package io.github.taetae98coding.jarvis.domain.calculator

class EvaluateExpressionUseCase {
    operator fun invoke(expression: String): CalculationResult = ExpressionEvaluator.evaluate(expression)
}

class CalculatePercentUseCase {
    operator fun invoke(mode: PercentMode, first: String, second: String): PercentResult =
        PercentCalculator.calculate(mode, first, second)
}

class CalculateBmiUseCase {
    operator fun invoke(heightCm: String, weightKg: String): BmiResult = BmiCalculator.calculate(heightCm, weightKg)
}
