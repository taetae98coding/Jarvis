package io.github.taetae98coding.jarvis.shared.platform

internal class InMemorySettingsStore(
    private val values: MutableMap<String, Boolean> = mutableMapOf(),
) : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}
