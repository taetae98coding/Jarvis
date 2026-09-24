package io.github.taetae98coding.jarvis.domain.terminal

enum class ClaudeActivity {
    Working,
    Finished,

    /** 턴을 끝내고 답을 기다리거나, 턴 도중에 사용자의 선택을 기다린다. */
    WaitingForInput,

    /** 멈췄거나 뜨지 못했거나 모르는 상태. */
    Idle,
}

/** Claude 탭 세션 하나의 지금 상태. [summary] 는 Claude 가 세션에 남긴 한 줄 요약이다. */
data class ClaudeStatus(
    val activity: ClaudeActivity,
    val summary: String? = null,
)
