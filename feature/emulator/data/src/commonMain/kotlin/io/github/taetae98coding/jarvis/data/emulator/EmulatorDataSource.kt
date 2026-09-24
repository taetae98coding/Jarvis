package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

internal interface EmulatorDataSource {
    fun observeStatus(): Flow<EmulatorStatus>

    fun observeDevices(): Flow<List<EmulatorDevice>>

    fun observeScreen(deviceId: String): Flow<EmulatorFrame?>

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

// 로컬 에이전트를 거치는 타깃의 프레임 폴링 주기. 에이전트는 요청마다 기기를 찍지 않고 메모리의 최신
// 프레임을 JPEG 로 인코딩(1080p 20~40ms)만 하므로 이 주기를 따라온다(docs/common/device-mirroring.html R11).
internal val EmulatorScreenPollInterval = 100.milliseconds

// iOS 시뮬레이터의 `simctl io screenshot` 은 한 장에 0.2~0.5초라 이보다 촘촘히 물어도 요청만 쌓인다.
internal val SimulatorScreenPollInterval = 500.milliseconds
