package io.github.taetae98coding.jarvis.ui.calculator

import io.github.taetae98coding.jarvis.domain.calculator.BmiCategory
import io.github.taetae98coding.jarvis.domain.calculator.CalculatorTab
import io.github.taetae98coding.jarvis.domain.calculator.ExpressionError
import io.github.taetae98coding.jarvis.domain.calculator.ExpressionEvaluator
import io.github.taetae98coding.jarvis.domain.calculator.PercentMode
import io.github.taetae98coding.jarvis.domain.calculator.PercentOperand
import io.github.taetae98coding.jarvis.domain.calculator.PercentResult

internal val CalculatorTab.label: String
    get() = when (this) {
        CalculatorTab.EXPRESSION -> "계산기"
        CalculatorTab.PERCENT -> "퍼센트"
        CalculatorTab.BMI -> "BMI"
    }

internal val ExpressionError.message: String
    get() = when (this) {
        ExpressionError.Incomplete -> "식이 끝나지 않았습니다"
        ExpressionError.MissingCloseParenthesis -> "괄호가 닫히지 않았습니다"
        is ExpressionError.UnmatchedCloseParenthesis -> "${index + 1}번째 글자의 닫는 괄호에 짝이 없습니다"
        is ExpressionError.UnexpectedCharacter -> "${index + 1}번째 글자 '$char' 를 읽을 수 없습니다"
        is ExpressionError.InvalidNumber -> "${index + 1}번째 글자에서 시작하는 숫자가 잘못되었습니다"
        ExpressionError.DivideByZero -> "0 으로 나눌 수 없습니다"
        ExpressionError.NotFinite -> "결과가 너무 크거나 정의되지 않습니다"
        ExpressionError.TooDeep -> "괄호가 ${ExpressionEvaluator.MaxDepth} 단계를 넘습니다"
    }

internal val PercentMode.title: String
    get() = when (this) {
        PercentMode.PERCENT_OF -> "X 의 Y% 는?"
        PercentMode.RATIO -> "X 는 Y 의 몇 %?"
        PercentMode.CHANGE -> "X 에서 Y 로 몇 % 증감?"
    }

internal val PercentMode.firstLabel: String
    get() = if (this == PercentMode.CHANGE) "X (처음)" else "X"

internal val PercentMode.secondLabel: String
    get() = when (this) {
        PercentMode.PERCENT_OF -> "Y (%)"
        PercentMode.RATIO -> "Y"
        PercentMode.CHANGE -> "Y (나중)"
    }

/** 답이 없으면(빈 칸) null. */
internal fun PercentMode.answer(result: PercentResult): String? =
    when (result) {
        PercentResult.Empty -> null
        is PercentResult.Value -> when (this) {
            PercentMode.PERCENT_OF -> "= ${result.text}"
            PercentMode.RATIO -> "= ${result.text}%"
            PercentMode.CHANGE -> when {
                result.value > 0 -> "${result.text}% 증가"
                result.value < 0 -> "${result.text}% 감소"
                else -> "변화 없음"
            }
        }
        is PercentResult.InvalidNumber -> when (result.operand) {
            PercentOperand.FIRST -> "X 가 숫자가 아닙니다"
            PercentOperand.SECOND -> "Y 가 숫자가 아닙니다"
        }
        PercentResult.DivideByZero -> if (this == PercentMode.CHANGE) "X 가 0 이면 증감률이 없습니다" else "Y 가 0 이면 셈할 수 없습니다"
        PercentResult.NotFinite -> "결과가 너무 큽니다"
    }

internal val BmiCategory.label: String
    get() = when (this) {
        BmiCategory.UNDERWEIGHT -> "저체중"
        BmiCategory.NORMAL -> "정상"
        BmiCategory.PRE_OBESE -> "비만 전단계"
        BmiCategory.OBESE_1 -> "1단계 비만"
        BmiCategory.OBESE_2 -> "2단계 비만"
        BmiCategory.OBESE_3 -> "3단계 비만"
    }

/** 대한비만학회 성인 기준표의 BMI 구간. */
internal val BmiCategory.range: String
    get() = when (this) {
        BmiCategory.UNDERWEIGHT -> "18.5 미만"
        BmiCategory.NORMAL -> "18.5–22.9"
        BmiCategory.PRE_OBESE -> "23–24.9"
        BmiCategory.OBESE_1 -> "25–29.9"
        BmiCategory.OBESE_2 -> "30–34.9"
        BmiCategory.OBESE_3 -> "35 이상"
    }

internal const val BmiCriteria = "대한비만학회 성인 기준"
