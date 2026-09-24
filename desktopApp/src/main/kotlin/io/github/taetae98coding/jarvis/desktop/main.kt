package io.github.taetae98coding.jarvis.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.taetae98coding.jarvis.shared.App
import io.github.taetae98coding.jarvis.shared.shutdownJarvisBrowser
import io.github.taetae98coding.jarvis.shared.startEmulatorHostAgent
import io.github.taetae98coding.jarvis.shared.startJarvisKoin
import io.github.taetae98coding.jarvis.shared.startJarvisMcpServer

// 에뮬레이터 개수를 세는 SDK 도구는 개발자 머신에서만 돌아간다. 데스크탑 앱은 그걸 할 수 있는
// 유일한 타깃이므로, 같은 머신의 에뮬레이터·시뮬레이터·브라우저에 결과를 넘겨 주는 에이전트를 함께
// 띄운다.
fun main() = startEmulatorHostAgent().use {
    startJarvisKoin()

    // Claude 탭이 붙는 MCP 서버. 도구가 Koin 의 이음새를 쓰므로 Koin 다음에 띄운다(docs/common/mcp-server.html).
    startJarvisMcpServer().use {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Jarvis",
                state = rememberWindowState(size = DpSize(900.dp, 640.dp)),
            ) {
                App()
            }
        }
    }
    shutdownJarvisBrowser()
}
