package io.github.taetae98coding.jarvis.shared.platform

internal data class EmulatorSummary(
    val total: Int = 0,
    val running: Int = 0,
)

internal data class EmulatorStatus(
    val android: EmulatorSummary = EmulatorSummary(),
    val ios: EmulatorSummary = EmulatorSummary(),
)

internal fun interface EmulatorProbe {
    suspend fun probe(): EmulatorStatus
}

/**
 * Counting virtual devices means driving the Android SDK and Xcode command line tools, so only a
 * target that can spawn processes on the developer machine reports real numbers. Every other
 * target answers with zeros rather than hiding the card, so the screen looks the same everywhere.
 */
internal expect val emulatorProbe: EmulatorProbe
