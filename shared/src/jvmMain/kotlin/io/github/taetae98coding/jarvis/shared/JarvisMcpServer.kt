package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.browser.BrowserEngine
import io.github.taetae98coding.jarvis.data.mcp.startMcpServer
import io.github.taetae98coding.jarvis.domain.mcp.McpToolbox
import org.koin.mp.KoinPlatformTools

/** [startJarvisKoin] 다음에 부른다. 도구 목록은 Koin 에 등록된 이음새로 정해진다. */
fun startJarvisMcpServer(): AutoCloseable = startMcpServer(KoinPlatformTools.defaultContext().get().get<McpToolbox>())

/** 앱이 끝날 때. 브라우저 엔진을 닫지 않고 끝내면 macOS 에서 Chromium 도우미 프로세스가 남는다. */
fun shutdownJarvisBrowser() {
    BrowserEngine.shutdown()
}
