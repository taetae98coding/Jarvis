package io.github.taetae98coding.jarvis.data.emulator.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

internal val androidHostAgentClient = HostAgentClient(
    fetch = ::fetchFromHostAgent,
    send = ::sendToHostAgent,
)

// 10.0.2.2 는 에뮬레이터가 호스트 머신의 루프백을 부르는 주소다. 실물 기기에는 그런 주소가 없어서
// `adb reverse tcp:47890 tcp:47890` 로 포워딩한 뒤 자기 루프백으로 닿는 경로를 함께 둔다.
private val HostAliases = listOf("10.0.2.2", "127.0.0.1")

// 에이전트는 개수와 목록을 미리 세 두고 응답하므로 빠르다. 여기서 오래 기다리는 건 에이전트가
// 없다는 뜻이다. 프레임만 호스트가 화면을 찍는 시간을 기다려 준다.
private const val TimeoutMillis = 2_000
private const val ScreenTimeoutMillis = 5_000

private suspend fun fetchFromHostAgent(path: String): ByteArray? =
    withContext(Dispatchers.IO) {
        HostAliases.firstNotNullOfOrNull { host -> request(hostAgentUrl(host, path), path) }
    }

private suspend fun sendToHostAgent(path: String, body: String) {
    withContext(Dispatchers.IO) {
        // 첫 번째로 응답한 호스트에서 멈춘다. 둘 다 살아 있을 일은 없지만, 같은 제스처를 두 번
        // 보내는 것은 두 번 눌리는 것과 같아서 성공하면 더 시도하지 않는다.
        HostAliases.firstOrNull { host -> post(hostAgentUrl(host, path), body) }
    }
}

private fun request(url: String, path: String): ByteArray? =
    runCatching {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = TimeoutMillis
            readTimeout = if (path.startsWith(HostAgentScreenPath)) ScreenTimeoutMillis else TimeoutMillis
        }

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use(InputStream::readBytes)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

private fun post(url: String, body: String): Boolean =
    runCatching {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            // 브라우저 클라이언트가 CORS 사전 요청을 피하려고 text/plain 으로 보낸다. 에이전트의
            // 분기를 하나로 두려고 여기서도 같은 타입을 쓴다.
            setRequestProperty("Content-Type", "text/plain")
        }

        try {
            connection.outputStream.use { it.write(body.encodeToByteArray()) }

            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)
