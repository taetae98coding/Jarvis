package io.github.taetae98coding.jarvis.data.emulator.agent

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.taetae98coding.jarvis.data.emulator.emulatorDataSource
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * 같은 머신의 다른 타깃에 에뮬레이터 개수를 알려주는 로컬 HTTP 에이전트를 띄운다.
 *
 * Android 에뮬레이터·iOS 시뮬레이터·브라우저는 샌드박스 안이라 SDK 도구를 직접 띄울 수 없다.
 * 개수를 셀 수 있는 건 개발자 머신에서 도는 데스크탑 앱뿐이어서, 그 결과를 HTTP 로 넘긴다.
 *
 * 포트가 이미 쓰이고 있으면 조용히 no-op 으로 빠진다. 데스크탑 앱 자신은 에이전트 없이도 개수를
 * 세므로 여기서 실패해도 앱 동작에는 영향이 없다.
 */
fun startEmulatorHostAgent(): AutoCloseable =
    startEmulatorHostAgent(HostAgentPort, emulatorDataSource.observeStatus())

internal fun startEmulatorHostAgent(
    port: Int,
    statuses: Flow<EmulatorStatus>,
): AutoCloseable {
    // 루프백에만 바인딩한다. 에뮬레이터의 10.0.2.2 와 `adb reverse` 는 호스트 루프백으로 들어오므로
    // 이걸로 충분하고, 같은 네트워크의 다른 기기에는 열리지 않는다.
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), port)
    val server = runCatching { HttpServer.create(address, 0) }.getOrNull() ?: return AutoCloseable {}

    val latest = AtomicReference<EmulatorStatus?>(null)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 요청마다 세지 않고 미리 세 둔 값을 준다. 구독자가 여럿이어도 SDK 도구는 한 번만 돌고, 응답이
    // 즉시 끝나므로 핸들러에 별도 스레드 풀이 필요 없다.
    scope.launch { statuses.collect(latest::set) }

    server.createContext(HostAgentPath) { exchange ->
        val status = latest.get()

        try {
            when {
                exchange.requestMethod != "GET" -> exchange.respond(MethodNotAllowed, null)
                // 아직 한 번도 세지 못했다. 0개로 답해서 "셀 수 없음" 과 섞이게 하지 않는다.
                status == null -> exchange.respond(ServiceUnavailable, null)
                else -> exchange.respond(Ok, encodeEmulatorStatus(status))
            }
        } finally {
            exchange.close()
        }
    }

    server.start()

    return AutoCloseable {
        server.stop(0)
        scope.cancel()
    }
}

private const val Ok = 200
private const val MethodNotAllowed = 405
private const val ServiceUnavailable = 503

private fun HttpExchange.respond(code: Int, body: String?) {
    val bytes = body?.encodeToByteArray()

    responseHeaders.add("Content-Type", "application/json")
    // 브라우저 클라이언트는 webApp 오리진과 포트가 달라 교차 출처가 된다. 단순 GET 이라 preflight 는
    // 없고 이 헤더 하나면 응답을 읽을 수 있다.
    responseHeaders.add("Access-Control-Allow-Origin", "*")
    // 개수는 계속 바뀌므로 브라우저와 NSURLSession 의 기본 캐시에 남아서는 안 된다.
    responseHeaders.add("Cache-Control", "no-store")

    // -1 이 "본문 없음" 이다. 0 을 주면 chunked 로 열려서 클라이언트가 오지 않는 본문을 기다린다.
    sendResponseHeaders(code, bytes?.size?.toLong() ?: -1L)
    bytes?.let { responseBody.use { stream -> stream.write(it) } }
}
