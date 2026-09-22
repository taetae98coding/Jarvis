package io.github.taetae98coding.jarvis.domain.screen

import kotlinx.coroutines.flow.StateFlow

class ObserveSystemScreenAwakeStatusUseCase(
    private val systemScreenAwake: SystemScreenAwakeRepository,
) {
    operator fun invoke(): StateFlow<SystemScreenAwakeStatus> = systemScreenAwake.status
}
