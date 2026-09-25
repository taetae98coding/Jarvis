package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.DeviceLogRepository
import kotlinx.coroutines.flow.Flow

internal interface DeviceLogDataSource {
    fun observeLog(deviceId: String): Flow<List<String>>
}

/** 기기 로그도 SDK 도구로만 읽을 수 있어서 [emulatorDataSource] 와 같이 JVM 만 직접 읽고 나머지는 에이전트에 묻는다. */
internal expect val deviceLogDataSource: DeviceLogDataSource

internal class DefaultDeviceLogRepository(
    private val dataSource: DeviceLogDataSource,
) : DeviceLogRepository {
    override fun observeLog(deviceId: String): Flow<List<String>> = dataSource.observeLog(deviceId)
}
