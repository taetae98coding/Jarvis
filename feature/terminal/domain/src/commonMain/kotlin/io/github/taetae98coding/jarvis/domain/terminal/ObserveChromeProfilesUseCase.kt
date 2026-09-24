package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

class ObserveChromeProfilesUseCase(
    private val repository: TerminalRepository,
) {
    operator fun invoke(): Flow<List<ChromeProfile>> = repository.observeChromeProfiles()
}
