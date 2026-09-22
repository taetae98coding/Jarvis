package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.Flow

internal data class EmulatorSummary(
    val total: Int = 0,
    val running: Int = 0,
)

internal data class EmulatorStatus(
    val android: EmulatorSummary = EmulatorSummary(),
    val ios: EmulatorSummary = EmulatorSummary(),
)

internal fun interface EmulatorProbe {
    fun observe(): Flow<EmulatorStatus>
}

/**
 * 가상 기기를 세려면 Android SDK 와 Xcode 커맨드라인 도구를 실행해야 해서, 개발자 머신에서 프로세스를
 * 띄울 수 있는 타깃만 실제 숫자를 낸다. 나머지 타깃은 카드를 숨기는 대신 0개로 답해서 화면 구성이
 * 플랫폼마다 갈라지지 않게 한다.
 */
internal expect val emulatorProbe: EmulatorProbe
