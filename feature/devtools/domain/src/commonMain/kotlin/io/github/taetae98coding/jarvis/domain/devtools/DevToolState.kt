package io.github.taetae98coding.jarvis.domain.devtools

/** 고른 도구와 그 도구에 저장된 입력. */
data class DevToolState(
    val tool: DevTool,
    val input: String,
)
