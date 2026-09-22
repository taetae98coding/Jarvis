package io.github.taetae98coding.jarvis.shared.platform

// An app sandboxed on a device — which is itself possibly the emulator being counted — has no view
// of the host machine's SDK tooling.
internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { EmulatorStatus() }
