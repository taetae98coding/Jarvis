package io.github.taetae98coding.jarvis.automation

/**
 * Claude 가 붙는 MCP 서버의 주소와, 호출한 Claude 탭을 가리키는 헤더(docs/common/mcp-server.html R1–R3).
 * 서버(:feature:mcp:data)와 Claude 를 띄우는 쪽(:feature:terminal:data)이 같은 값을 써야 해서 여기 둔다.
 */
object AgentServer {
    // 앱을 다시 켜도 살아 있던 백그라운드 세션이 그대로 붙도록 고정한다. 호스트 에이전트(47890) 바로 다음 번호다.
    const val Port: Int = 47891

    const val Path: String = "/mcp"

    const val Name: String = "jarvis"

    const val SessionHeader: String = "X-Jarvis-Session"

    val Url: String = "http://127.0.0.1:$Port$Path"
}
