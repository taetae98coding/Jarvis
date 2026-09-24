package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.emulator.agent.webHostAgentClient
import io.github.taetae98coding.jarvis.data.emulator.agent.hostAgentDevicePairingDataSource

internal actual val devicePairingDataSource: DevicePairingDataSource =
    hostAgentDevicePairingDataSource(webHostAgentClient)
