package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeEmulatorProbe(initial: EmulatorStatus = EmulatorStatus()) : EmulatorProbe {
    val status = MutableStateFlow(initial)

    override fun observe(): Flow<EmulatorStatus> = status
}

internal fun fakeEmulatorProbe(
    android: EmulatorSummary = EmulatorSummary(),
    ios: EmulatorSummary = EmulatorSummary(),
): FakeEmulatorProbe = FakeEmulatorProbe(EmulatorStatus(android = android, ios = ios))
