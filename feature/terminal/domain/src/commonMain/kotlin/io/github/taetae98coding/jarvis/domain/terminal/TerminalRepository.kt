package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

interface TerminalRepository {
    /** 이 플랫폼에서 셸을 띄울 수 있는지. 실행 중에 바뀌지 않는다. */
    val isSupported: Boolean

    /** [TerminalProgram.Claude] 를 띄울 수 있는지. 실행 중에 바뀌지 않는다. */
    val isClaudeSupported: Boolean

    /** [TerminalProgram.Browser] 탭을 열 수 있는지. 실행 중에 바뀌지 않는다. */
    val isBrowserSupported: Boolean

    /** 이 PC 의 Chrome 쿠키를 브라우저 탭으로 가져올 수 있는지(macOS 데스크톱만). 실행 중에 바뀌지 않는다. */
    val isChromeImportSupported: Boolean

    /**
     * 이 PC 의 Chrome 프로필 목록. 수집할 때 `Local State` 를 읽어 한 번 내보낸다(cold). 지원하지 않는
     * 플랫폼에서는 빈 목록이다. 드롭다운을 열 때 [kotlinx.coroutines.flow.first] 로 지금 값을 읽는다.
     */
    fun observeChromeProfiles(): Flow<List<ChromeProfile>>

    /**
     * [profileDirectory] 프로필의 모든 사이트 쿠키를 복호화해 돌려준다. 키체인 접근을 허용하지 않거나
     * 읽지 못하면 빈 목록이다(no-op). 넣는 것은 UI 가 웹뷰 쿠키 저장소에 한다.
     */
    suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie>

    /**
     * [tab] 이 가리키는 것을 띄운다. 셸은 [TerminalTab.directory] 에서 시작하고, Claude 는
     * [TerminalTab.claudeSessionId] 의 백그라운드 세션에 붙는다(없으면 만든다).
     *
     * 띄우지 못하면 null 이다. 지원하지 않는 플랫폼에서는 언제나 null 이다.
     */
    suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession?

    /** 앱 밖에서 도는 Claude 백그라운드 세션을 멈춘다. 대화 기록은 남는다. 없는 세션이면 아무 일도 없다. */
    suspend fun stopClaude(sessionId: String)

    /** 운영체제 알림을 하나 보낸다. 보낼 수 없는 환경이면 아무 일도 없다. */
    suspend fun showNotification(notification: ClaudeNotification)
}
