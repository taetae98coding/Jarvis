package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow

interface EmulatorRepository {
    fun observeStatus(): Flow<EmulatorStatus>

    fun observeDevices(): Flow<List<EmulatorDevice>>

    /**
     * 기기 화면을 PNG 한 장씩 흘려보낸다. 그 주기에 화면을 찍지 못했으면 null 이다.
     *
     * 구독을 끊으면 촬영도 멈춘다. 보이지 않는 기기의 화면을 계속 찍지 않으려면 화면을 벗어날 때
     * 수집을 끝내야 한다.
     */
    fun observeScreen(deviceId: String): Flow<ByteArray?>

    suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture)
}
