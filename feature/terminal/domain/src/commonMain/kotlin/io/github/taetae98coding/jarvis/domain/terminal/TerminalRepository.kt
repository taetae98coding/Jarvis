package io.github.taetae98coding.jarvis.domain.terminal

interface TerminalRepository {
    /** 이 플랫폼에서 셸을 띄울 수 있는지. 실행 중에 바뀌지 않는다. */
    val isSupported: Boolean

    /** [TerminalProgram.Claude] 를 띄울 수 있는지. 실행 중에 바뀌지 않는다. */
    val isClaudeSupported: Boolean

    /**
     * [tab] 이 가리키는 것을 띄운다. 셸은 [TerminalTab.directory] 에서 시작하고, Claude 는
     * [TerminalTab.claudeSessionId] 의 백그라운드 세션에 붙는다(없으면 만든다).
     *
     * 띄우지 못하면 null 이다. 지원하지 않는 플랫폼에서는 언제나 null 이다.
     */
    suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession?

    /** 앱 밖에서 도는 Claude 백그라운드 세션을 멈춘다. 대화 기록은 남는다. 없는 세션이면 아무 일도 없다. */
    suspend fun stopClaude(sessionId: String)
}
