package io.github.taetae98coding.jarvis.domain.calculator

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val calculatorDomainModule = module {
    factoryOf(::ObserveCalculatorTabUseCase)
    factoryOf(::SelectCalculatorTabUseCase)
    factoryOf(::ObserveCalculatorInputsUseCase)
    factoryOf(::SetCalculatorInputUseCase)
    factoryOf(::EvaluateExpressionUseCase)
    factoryOf(::CalculatePercentUseCase)
    factoryOf(::CalculateBmiUseCase)
    factoryOf(::ObserveCalculationHistoryUseCase)
    factoryOf(::AddCalculationHistoryUseCase)
    factoryOf(::ClearCalculationHistoryUseCase)
}
