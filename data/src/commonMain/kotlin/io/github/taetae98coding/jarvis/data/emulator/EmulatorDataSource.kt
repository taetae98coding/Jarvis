package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.flow.Flow

internal fun interface EmulatorDataSource {
    fun observeStatus(): Flow<EmulatorStatus>
}

/**
 * 가상 기기를 세려면 Android SDK 와 Xcode 커맨드라인 도구를 실행해야 한다. 그걸 할 수 있는 건
 * 개발자 머신에서 도는 JVM 타깃뿐이고, 나머지 타깃은 hostAgentEmulatorDataSource 로 같은 머신의
 * 데스크탑 앱에 물어본다.
 */
internal expect val emulatorDataSource: EmulatorDataSource
