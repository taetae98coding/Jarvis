package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

interface DevicePairingRepository {
    /** 페어링을 기다리는 기기. 개발자 머신이 찾을 방법이 없으면 null 이다. */
    fun observePairingServices(): Flow<List<PairingService>?>

    /** 페어링하고, 붙을 때까지 잠시 기다린다. 수십 초가 걸릴 수 있다. */
    suspend fun pair(service: PairingService, code: String): PairingResult
}
