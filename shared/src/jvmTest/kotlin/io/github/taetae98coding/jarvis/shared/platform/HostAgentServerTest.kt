package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class HostAgentServerTest {
    @Test
    fun servesTheLatestCountOverHttp() {
        val status = EmulatorStatus(android = EmulatorSummary(total = 5, running = 2), ios = null)

        withAgent(MutableStateFlow(status)) { port ->
            assertEquals(status, decodeEmulatorStatus(awaitBody(port)))
        }
    }

    @Test
    fun answersServiceUnavailableBeforeTheFirstCount() {
        // 아직 세지 못한 상태를 0개로 답하면 클라이언트가 "셀 수 없음" 과 구분할 수 없다.
        withAgent(emptyFlow()) { port ->
            assertEquals(503, request(port, "GET").code)
        }
    }

    @Test
    fun rejectsOtherMethods() {
        withAgent(MutableStateFlow(EmulatorStatus())) { port ->
            assertEquals(405, request(port, "POST").code)
        }
    }

    @Test
    fun letsBrowserOriginsReadTheResponse() {
        // webApp 은 다른 포트에서 서빙되므로 CORS 헤더가 없으면 브라우저가 응답을 읽지 못한다.
        withAgent(MutableStateFlow(EmulatorStatus())) { port ->
            awaitBody(port)

            assertEquals("*", request(port, "GET").corsOrigin)
        }
    }

    @Test
    fun staysSilentWhenThePortIsTaken() {
        val port = freePort()

        startEmulatorHostAgent(port, MutableStateFlow(EmulatorStatus())).use {
            // 두 번째 에이전트는 예외를 던지지 않고 아무것도 하지 않는다.
            startEmulatorHostAgent(port, MutableStateFlow(EmulatorStatus())).close()

            assertEquals(200, request(port, "GET").code)
        }
    }

    private fun withAgent(statuses: Flow<EmulatorStatus>, block: (Int) -> Unit) {
        val port = freePort()

        startEmulatorHostAgent(port, statuses).use { block(port) }
    }

    // 에이전트는 개수를 백그라운드에서 받아 두므로 첫 요청이 503 일 수 있다.
    private fun awaitBody(port: Int): String {
        repeat(20) {
            val response = request(port, "GET")

            if (response.code == 200 && response.body != null) return response.body

            Thread.sleep(50)
        }

        fail("에이전트가 개수를 응답하지 않았다")
    }

    private fun request(port: Int, method: String): AgentResponse {
        val url = URI("http://127.0.0.1:$port$HostAgentPath").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply { requestMethod = method }

        try {
            val code = connection.responseCode
            val body = if (code == 200) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                null
            }

            return AgentResponse(code, body, connection.getHeaderField("Access-Control-Allow-Origin"))
        } finally {
            connection.disconnect()
        }
    }

    // 에이전트를 띄우기 직전에 포트를 놓아주는 방식이라 이론적으로는 경합이 있다. 고정 포트를 쓰면
    // 개발자 머신에서 실제로 도는 에이전트와 부딪히므로 이쪽을 택했다.
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private class AgentResponse(
        val code: Int,
        val body: String?,
        val corsOrigin: String?,
    )
}
