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

    /**
     * 꺼져 있는 가상 기기를 켠다. 요청만 보내고 켜졌는지는 확인하지 않는다. 뜨는 데 수십 초가
     * 걸리는데 기다릴 방법이 없어서, 떴다는 사실은 [observeDevices] 만이 알려준다.
     */
    suspend fun launch(deviceId: String)

    /** 연결된 기기의 꺼진 화면을 켠다. 이미 켜져 있으면 아무 일도 일어나지 않는다. */
    suspend fun wake(deviceId: String)
}
