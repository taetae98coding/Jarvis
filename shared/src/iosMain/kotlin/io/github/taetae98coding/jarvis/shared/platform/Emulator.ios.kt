package io.github.taetae98coding.jarvis.shared.platform

// iOS has no process spawning API for third party apps, so `simctl` and `adb` are out of reach even
// when the app happens to run on a Mac's simulator.
internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { EmulatorStatus() }
