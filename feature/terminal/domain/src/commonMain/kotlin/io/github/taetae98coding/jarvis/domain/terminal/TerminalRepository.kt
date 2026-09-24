package io.github.taetae98coding.jarvis.domain.terminal

interface TerminalRepository {
    /** 이 플랫폼에서 셸을 띄울 수 있는지. 실행 중에 바뀌지 않는다. */
    val isSupported: Boolean

    /** 셸을 띄우지 못하면 null 이다. 지원하지 않는 플랫폼에서는 언제나 null 이다. */
    suspend fun open(size: TerminalSize): TerminalSession?
}
