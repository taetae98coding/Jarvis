package io.github.taetae98coding.jarvis.data.devtools

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import io.github.taetae98coding.jarvis.domain.devtools.DevToolsSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultDevToolsSettingsRepository(
    private val store: SettingsStore,
) : DevToolsSettingsRepository {
    override fun observeSelectedTool(): Flow<DevTool> =
        store.observeString(SelectedToolKey, DevTool.TIMESTAMP.storedValue).map(DevTool::fromStored)

    override fun readSelectedTool(): DevTool =
        DevTool.fromStored(store.getString(SelectedToolKey, DevTool.TIMESTAMP.storedValue))

    override fun setSelectedTool(tool: DevTool) {
        store.putString(SelectedToolKey, tool.storedValue)
    }

    override fun observeInput(tool: DevTool): Flow<String> = store.observeString(inputKey(tool), "")

    override fun readInput(tool: DevTool): String = store.getString(inputKey(tool), "")

    override fun setInput(tool: DevTool, input: String) {
        store.putString(inputKey(tool), input)
    }

    internal companion object {
        const val SelectedToolKey = "devtools_selected_tool"

        fun inputKey(tool: DevTool): String = "devtools_input_${tool.storedValue}"
    }
}
