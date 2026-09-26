package io.github.taetae98coding.jarvis.domain.devtools

class SelectDevToolUseCase(
    private val settings: DevToolsSettingsRepository,
) {
    operator fun invoke(tool: DevTool) {
        settings.setSelectedTool(tool)
    }
}
