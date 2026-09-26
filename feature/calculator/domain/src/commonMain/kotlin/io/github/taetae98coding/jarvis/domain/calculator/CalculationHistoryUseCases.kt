package io.github.taetae98coding.jarvis.domain.calculator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CalculationHistoryEntry(
    val expression: String,
    val resultText: String,
)

object CalculationHistory {
    const val MaxEntries = 20

    private val Whitespace = Regex("\\s+")

    /** 저장소에 적는 꼴. 줄바꿈·탭이 한 칸 공백이 되어 저장소의 줄 구분과 부딪히지 않는다. */
    fun normalize(expression: String): String = expression.trim().replace(Whitespace, " ")

    /** 답을 저장하지 않고 수식을 다시 셈해 붙인다. 셈할 수 없는 줄(앱 밖에서 고친 값)은 버린다. */
    internal fun entries(expressions: List<String>): List<CalculationHistoryEntry> =
        expressions.mapNotNull { expression ->
            (ExpressionEvaluator.evaluate(expression) as? CalculationResult.Value)?.let { CalculationHistoryEntry(expression, it.text) }
        }
}

class ObserveCalculationHistoryUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    operator fun invoke(scope: CoroutineScope): StateFlow<List<CalculationHistoryEntry>> =
        settings.observeHistory()
            .map(CalculationHistory::entries)
            .stateIn(scope, SharingStarted.WhileSubscribed(), CalculationHistory.entries(settings.readHistory()))
}

/** 셈할 수 있는 수식만 맨 위에 넣는다. 같은 수식이 있으면 옛 줄을 지운다. */
class AddCalculationHistoryUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    suspend operator fun invoke(expression: String) {
        val normalized = CalculationHistory.normalize(expression)
        if (ExpressionEvaluator.evaluate(normalized) !is CalculationResult.Value) return

        val current = settings.observeHistory().first()
        settings.setHistory((listOf(normalized) + current.filter { it != normalized }).take(CalculationHistory.MaxEntries))
    }
}

class ClearCalculationHistoryUseCase(
    private val settings: CalculatorSettingsRepository,
) {
    operator fun invoke() {
        settings.setHistory(emptyList())
    }
}
