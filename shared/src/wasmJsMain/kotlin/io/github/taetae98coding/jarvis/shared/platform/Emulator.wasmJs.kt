package io.github.taetae98coding.jarvis.shared.platform

// The browser sandbox has no filesystem or process access; reaching the host's SDK would take a
// companion server, which this app does not have.
internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { EmulatorStatus() }
