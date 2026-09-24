package io.github.taetae98coding.jarvis.automation

/**
 * 탭 id 로 가리키는 앱 안 브라우저 페이지를 조작한다(docs/common/mcp-server.html R9–R12). 좌표는 CSS 픽셀이고
 * [screenshot] 의 이미지 픽셀과 같다. 구현은 브라우저 엔진이 있는 타깃만 Koin 에 등록한다.
 */
interface BrowserAutomation {
    /** 그 탭의 페이지가 없으면 [initialUrl] 로 만든다. 지금 주소·제목을 준다. */
    suspend fun page(tabId: Long, initialUrl: String): BrowserPageInfo

    suspend fun navigate(tabId: Long, url: String)

    suspend fun back(tabId: Long)

    suspend fun screenshot(tabId: Long): AutomationImage

    /** 주소·제목, 보이는 상호작용 요소의 역할·이름·가운데 좌표, 본문 앞부분을 사람이 읽는 글로. */
    suspend fun snapshot(tabId: Long): String

    suspend fun click(tabId: Long, x: Int, y: Int, clickCount: Int)

    suspend fun type(tabId: Long, text: String)

    suspend fun pressKey(tabId: Long, key: String)

    /** [x]·[y] 가 null 이면 뷰포트 가운데다. [deltaY] 가 양수면 아래로. */
    suspend fun scroll(tabId: Long, x: Int?, y: Int?, deltaY: Int)

    /** 결과를 JSON 글자로 준다. Promise 는 기다린다. */
    suspend fun evaluate(tabId: Long, expression: String): String

    /** 페이지를 닫는다. 없으면 아무 일도 하지 않는다. */
    fun close(tabId: Long)
}

data class BrowserPageInfo(
    val url: String,
    val title: String?,
)
