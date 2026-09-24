package io.github.taetae98coding.jarvis.automation

/**
 * Claude 가 쓰는 브라우저·기기를 그 Claude 탭이 있는 패널에 탭으로 붙인다(docs/common/mcp-server.html R3–R8).
 * [sessionId] 는 도구를 부른 Claude 탭의 `claudeSessionId` 다. 구현은 터미널 기능이 Koin 에 등록한다.
 */
interface AgentTabs {
    /** 그 Claude 탭이 있는 패널. 없으면 브라우저 도구를 쓸 수 없고(R7) 기기를 할당받지 못한다. */
    suspend fun callerPanel(sessionId: String): AgentPanel?

    /** Claude 탭이 하나라도 있는 패널. 여기 없는 패널의 기기 할당은 풀린 것으로 친다(docs/common/device-lease.html R12). */
    suspend fun claudePanels(): List<AgentPanel>

    /** 호출한 그룹의 탭 줄 끝에 선택하지 않은 브라우저 탭을 붙이고 그 id 를 준다. 호출한 탭이 없으면 null. */
    suspend fun openBrowserTab(sessionId: String, url: String): Long?

    /** 호출한 패널의 브라우저 탭. 호출한 탭이 없으면 빈 목록이다. */
    suspend fun browserTabs(sessionId: String): List<AgentBrowserTab>

    /** 호출한 패널에 [deviceId] 기기 탭이 없으면 호출한 그룹에 선택하지 않고 붙인다. [platform] 은 탭 아이콘에 쓴다. */
    suspend fun showDevice(sessionId: String, deviceId: String, deviceName: String, platform: AutomationPlatform)

    /** 호출한 패널의 탭이면 닫는다. 닫았으면 true. */
    suspend fun closeTab(sessionId: String, tabId: Long): Boolean
}

/** [url] 은 탭에 저장된 마지막 주소다. */
data class AgentBrowserTab(
    val tabId: Long,
    val url: String,
)

/** 기기를 할당받는 단위. [name] 은 다른 패널이 그 기기를 쓰려 할 때 오류 문구에 나온다. */
data class AgentPanel(
    val id: Long,
    val name: String,
)
