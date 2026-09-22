package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
internal data class EmulatorSummary(
    val total: Int = 0,
    val running: Int = 0,
)

/**
 * null 인 항목은 "0개" 가 아니라 "이 타깃에서는 셀 수 없음" 이다. 둘이 화면에서 같아 보이면
 * SDK 도구를 못 찾은 것과 에뮬레이터가 없는 것을 구분할 수 없어서 따로 둔다.
 */
@Serializable
internal data class EmulatorStatus(
    val android: EmulatorSummary? = null,
    val ios: EmulatorSummary? = null,
)

internal fun interface EmulatorProbe {
    fun observe(): Flow<EmulatorStatus>
}

/**
 * 가상 기기를 세려면 Android SDK 와 Xcode 커맨드라인 도구를 실행해야 한다. 그걸 할 수 있는 건
 * 개발자 머신에서 도는 JVM 타깃뿐이고, 나머지 타깃은 [hostAgentEmulatorProbe] 로 같은 머신의
 * 데스크탑 앱에 물어본다.
 */
internal expect val emulatorProbe: EmulatorProbe
