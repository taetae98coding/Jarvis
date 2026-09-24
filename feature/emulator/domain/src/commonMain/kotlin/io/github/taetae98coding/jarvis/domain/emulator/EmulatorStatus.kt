package io.github.taetae98coding.jarvis.domain.emulator

/**
 * [total]·[running] 은 가상 기기만, [physical] 은 연결된 실물 기기만 센다. 실물 기기는 꽂혀 있을 때만
 * 보여서 실행 중과 전체가 늘 같으므로 한 숫자만 둔다.
 */
data class EmulatorSummary(
    val total: Int = 0,
    val running: Int = 0,
    val physical: Int = 0,
)

/**
 * null 인 항목은 "0개" 가 아니라 "이 타깃에서는 셀 수 없음" 이다. 둘이 화면에서 같아 보이면
 * SDK 도구를 못 찾은 것과 에뮬레이터가 없는 것을 구분할 수 없어서 따로 둔다.
 */
data class EmulatorStatus(
    val android: EmulatorSummary? = null,
    val ios: EmulatorSummary? = null,
)
