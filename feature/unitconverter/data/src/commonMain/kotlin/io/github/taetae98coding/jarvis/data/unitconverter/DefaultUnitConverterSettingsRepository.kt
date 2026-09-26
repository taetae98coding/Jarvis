package io.github.taetae98coding.jarvis.data.unitconverter

import io.github.taetae98coding.jarvis.data.settings.SettingsPollInterval
import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.data.state.observeSystemState
import io.github.taetae98coding.jarvis.domain.unitconverter.MeasureUnit
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitCategory
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitConverterSettingsRepository
import io.github.taetae98coding.jarvis.domain.unitconverter.UnitSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultUnitConverterSettingsRepository(
    private val store: SettingsStore,
) : UnitConverterSettingsRepository {
    override fun observeCategory(): Flow<UnitCategory> =
        store.observeString(CategoryKey, UnitCategory.LENGTH.storedValue).map(UnitCategory::fromStored)

    override fun readCategory(): UnitCategory = UnitCategory.fromStored(store.getString(CategoryKey, UnitCategory.LENGTH.storedValue))

    override fun setCategory(category: UnitCategory) {
        store.putString(CategoryKey, category.storedValue)
    }

    // 세 키를 observeString 셋으로 combine 하면 리스너가 셋 붙는다. 한 신호에 세 키를 함께 다시 읽는다.
    override fun observeSelection(category: UnitCategory): Flow<UnitSelection> =
        observeSystemState(signals = store.changes, interval = SettingsPollInterval) { readSelection(category) }

    override fun readSelection(category: UnitCategory): UnitSelection =
        UnitSelection(
            from = MeasureUnit.fromStored(category, store.getString(fromKey(category), "")) ?: category.defaultFrom,
            to = MeasureUnit.fromStored(category, store.getString(toKey(category), "")) ?: category.defaultTo,
            input = store.getString(inputKey(category), ""),
        )

    override fun setUnits(category: UnitCategory, from: MeasureUnit, to: MeasureUnit) {
        store.putString(fromKey(category), from.storedValue)
        store.putString(toKey(category), to.storedValue)
    }

    override fun setInput(category: UnitCategory, input: String) {
        store.putString(inputKey(category), input)
    }

    internal companion object {
        const val CategoryKey = "unitconverter_category"

        fun fromKey(category: UnitCategory): String = "unitconverter_${category.storedValue}_from"

        fun toKey(category: UnitCategory): String = "unitconverter_${category.storedValue}_to"

        fun inputKey(category: UnitCategory): String = "unitconverter_${category.storedValue}_input"
    }
}
