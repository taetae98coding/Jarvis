package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.time.Clock

class GetCurrentEpochSecondsUseCase(
    private val clock: Clock = Clock.System,
) {
    operator fun invoke(): Long = clock.now().epochSeconds
}
