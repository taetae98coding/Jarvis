package io.github.taetae98coding.jarvis.data.mcp

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.taetae98coding.jarvis.automation.AgentServer
import io.github.taetae98coding.jarvis.domain.mcp.McpToolbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * Claude 가 붙는 MCP 서버를 띄운다(docs/common/mcp-server.html R1·R4, 프로토콜 표). 포트가 이미 쓰이고 있으면
 * 조용히 서버 없이 돌아온다 — 앱은 서버 없이도 동작한다.
 */
fun startMcpServer(toolbox: McpToolbox, port: Int = AgentServer.Port): AutoCloseable {
    // 루프백에만 묶는다. 같은 네트워크의 다른 기기에서는 닿지 않는다.
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), port)
    val server = runCatching { HttpServer.create(address, 0) }.getOrNull() ?: return AutoCloseable {}
    val protocol = McpProtocol(toolbox)

    // 도구 하나가 수십 초(WebDriverAgent 준비는 수 분) 걸릴 수 있다. 기본 실행기는 단일 스레드라 그동안 다른
    // 세션의 요청이 모두 막히므로 요청마다 스레드를 준다.
    val executor = Executors.newCachedThreadPool { runnable -> Thread(runnable, "jarvis-mcp").apply { isDaemon = true } }
    server.executor = executor

    server.createContext(AgentServer.Path) { exchange ->
        exchange.use {
            when {
                // 브라우저 안 페이지가 보낸 요청이다(DNS rebinding). Claude Code 는 Origin 을 붙이지 않는다.
                exchange.requestHeaders.getFirst("Origin") != null -> exchange.respond(Forbidden)
                exchange.requestMethod != "POST" -> {
                    exchange.responseHeaders.add("Allow", "POST")
                    exchange.respond(MethodNotAllowed)
                }
                else -> {
                    val body = exchange.requestBody.readBytes().decodeToString()
                    val sessionId = exchange.requestHeaders.getFirst(AgentServer.SessionHeader)?.takeIf { it.isNotBlank() }
                    // 핸들러 스레드는 요청 하나의 것이라 여기서 기다려도 다른 요청을 막지 않는다.
                    val response = runBlocking(Dispatchers.IO) { protocol.handle(body, sessionId) }

                    if (response == null) {
                        exchange.respond(Accepted)
                    } else {
                        exchange.responseHeaders.add("Content-Type", "application/json")
                        exchange.respond(Ok, response.encodeToByteArray())
                    }
                }
            }
        }
    }

    server.start()

    return AutoCloseable {
        server.stop(0)
        executor.shutdownNow()
    }
}

private fun HttpExchange.respond(status: Int, body: ByteArray? = null) {
    sendResponseHeaders(status, body?.size?.toLong() ?: -1)
    body?.let { responseBody.write(it) }
}

private const val Ok = 200
private const val Accepted = 202
private const val Forbidden = 403
private const val MethodNotAllowed = 405
