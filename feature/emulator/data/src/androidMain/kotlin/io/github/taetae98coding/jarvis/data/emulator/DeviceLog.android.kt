package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.emulator.agent.androidHostAgentClient
import io.github.taetae98coding.jarvis.data.emulator.agent.hostAgentDeviceLogDataSource

internal actual val deviceLogDataSource: DeviceLogDataSource = hostAgentDeviceLogDataSource(androidHostAgentClient)
