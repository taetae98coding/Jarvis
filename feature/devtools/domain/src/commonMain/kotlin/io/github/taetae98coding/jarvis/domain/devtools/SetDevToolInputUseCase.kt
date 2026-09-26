package io.github.taetae98coding.jarvis.domain.devtools

class SetDevToolInputUseCase(
    private val settings: DevToolsSettingsRepository,
) {
    operator fun invoke(tool: DevTool, input: String) {
        settings.setInput(tool, input)
    }
}
