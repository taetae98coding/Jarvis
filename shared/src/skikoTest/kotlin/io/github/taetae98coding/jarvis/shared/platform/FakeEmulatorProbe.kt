package io.github.taetae98coding.jarvis.shared.platform

internal fun fakeEmulatorProbe(
    android: EmulatorSummary = EmulatorSummary(),
    ios: EmulatorSummary = EmulatorSummary(),
): EmulatorProbe = EmulatorProbe { EmulatorStatus(android = android, ios = ios) }
