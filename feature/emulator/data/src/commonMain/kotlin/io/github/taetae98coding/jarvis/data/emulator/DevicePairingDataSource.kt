package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

internal interface DevicePairingDataSource {
    fun observePairingServices(): Flow<List<PairingService>?>

    suspend fun pair(service: PairingService, code: String): PairingResult
}

/**
 * 페어링하는 것은 기기와 같은 네트워크에서 mDNS 를 보는 개발자 머신의 `adb` 다. JVM 은 직접 부르고,
 * 나머지 타깃은 에뮬레이터와 같은 에이전트에 묻는다.
 */
internal expect val devicePairingDataSource: DevicePairingDataSource

// 사용자가 기기에서 페어링 창을 연 채 기다리고 있다. Android Studio 도 1초마다 다시 본다.
internal val PairingServicePollInterval = 1.seconds
