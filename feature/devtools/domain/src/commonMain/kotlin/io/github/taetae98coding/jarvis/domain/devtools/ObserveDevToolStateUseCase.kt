package io.github.taetae98coding.jarvis.domain.devtools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ObserveDevToolStateUseCase(
    private val settings: DevToolsSettingsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(scope: CoroutineScope): StateFlow<DevToolState> {
        val tool = settings.readSelectedTool()

        return settings.observeSelectedTool()
            .flatMapLatest { selected -> settings.observeInput(selected).map { DevToolState(selected, it) } }
            .stateIn(scope, SharingStarted.WhileSubscribed(), DevToolState(tool, settings.readInput(tool)))
    }
}
