package io.github.taetae98coding.jarvis.domain.unitconverter

import kotlinx.coroutines.flow.Flow

/**
 * 마지막에 고른 분류와 분류마다의 단위·입력. 앱을 껐다 켜도 남는다.
 *
 * `observe*` 는 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. `read*` 는 첫 프레임에 쓸 초기값이다.
 * 돌려주는 단위는 늘 그 분류의 단위다.
 */
interface UnitConverterSettingsRepository {
    fun observeCategory(): Flow<UnitCategory>

    fun readCategory(): UnitCategory

    fun setCategory(category: UnitCategory)

    fun observeSelection(category: UnitCategory): Flow<UnitSelection>

    fun readSelection(category: UnitCategory): UnitSelection

    fun setUnits(category: UnitCategory, from: MeasureUnit, to: MeasureUnit)

    fun setInput(category: UnitCategory, input: String)
}
