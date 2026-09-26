package io.github.taetae98coding.jarvis.domain.calculator

import kotlinx.coroutines.flow.Flow

/**
 * 마지막에 고른 탭, 칸마다의 입력, 최근 계산한 수식. 앱을 껐다 켜도 남는다.
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 */
interface CalculatorSettingsRepository {
    fun observeSelectedTab(): Flow<CalculatorTab>

    fun readSelectedTab(): CalculatorTab

    fun setSelectedTab(tab: CalculatorTab)

    fun observeInput(field: CalculatorField): Flow<String>

    fun readInput(field: CalculatorField): String

    fun setInput(field: CalculatorField, input: String)

    /** 새것부터. 줄바꿈이 없는 수식만 담긴다. */
    fun observeHistory(): Flow<List<String>>

    fun readHistory(): List<String>

    fun setHistory(expressions: List<String>)
}
