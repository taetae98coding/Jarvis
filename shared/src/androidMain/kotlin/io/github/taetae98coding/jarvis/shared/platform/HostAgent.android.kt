package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI

internal suspend fun fetchHostAgentEmulators(): EmulatorStatus? =
    withContext(Dispatchers.IO) {
        HostAliases.firstNotNullOfOrNull { host -> requestEmulators(hostAgentUrl(host)) }
    }

// 10.0.2.2 는 에뮬레이터가 호스트 머신의 루프백을 부르는 주소다. 실물 기기에는 그런 주소가 없어서
// `adb reverse tcp:47890 tcp:47890` 로 포워딩한 뒤 자기 루프백으로 닿는 경로를 함께 둔다.
private val HostAliases = listOf("10.0.2.2", "127.0.0.1")

// 에이전트는 값을 미리 세 두고 응답하므로 빠르다. 여기서 오래 기다리는 건 에이전트가 없다는 뜻이다.
private const val TimeoutMillis = 1_000

private fun requestEmulators(url: String): EmulatorStatus? =
    runCatching {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
        }

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
        ?.let(::decodeEmulatorStatus)
