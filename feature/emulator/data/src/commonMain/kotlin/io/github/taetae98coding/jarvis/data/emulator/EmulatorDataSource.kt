package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

internal interface EmulatorDataSource {
    fun observeStatus(): Flow<EmulatorStatus>

    fun observeDevices(): Flow<List<EmulatorDevice>>

    fun observeScreen(deviceId: String): Flow<ByteArray?>

    suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture)

    suspend fun launch(deviceId: String)

    suspend fun wake(deviceId: String)
}

/**
 * 가상 기기를 세거나 들여다보려면 Android SDK 와 Xcode 커맨드라인 도구를 실행해야 한다. 그걸 할 수
 * 있는 건 개발자 머신에서 도는 JVM 타깃뿐이고, 나머지 타깃은 hostAgentEmulatorDataSource 로 같은
 * 머신의 데스크탑 앱에 물어본다.
 */
internal expect val emulatorDataSource: EmulatorDataSource

// 화면 한 장을 찍는 데 이보다 오래 걸리므로(Android screencap 기준 0.3~1초) 실제 갱신은 더 느리다.
// 주기를 더 줄여도 촬영이 따라오지 못하고 요청만 쌓인다.
internal val EmulatorScreenPollInterval = 500.milliseconds
