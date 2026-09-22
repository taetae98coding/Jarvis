package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.data.emulator.EmulatorDataSource
import io.github.taetae98coding.jarvis.data.emulator.EmulatorScreenPollInterval
import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

/**
 * 데스크탑 앱이 띄우는 로컬 HTTP 에이전트의 주소.
 *
 * 등록되지 않은 대역에서 고정 포트를 하나 골랐다. 클라이언트가 포트를 미리 알아야 해서 OS 가
 * 골라주는 임의 포트를 쓸 수 없다.
 */
internal const val HostAgentPort: Int = 47890
internal const val HostAgentPath: String = "/emulators"
internal const val HostAgentDevicesPath: String = "$HostAgentPath/devices"
internal const val HostAgentScreenPath: String = "$HostAgentPath/screen"
internal const val HostAgentGesturePath: String = "$HostAgentPath/gesture"

internal fun hostAgentUrl(host: String, path: String = HostAgentPath): String =
    "http://$host:$HostAgentPort$path"

internal fun hostAgentScreenPath(deviceId: String): String =
    "$HostAgentScreenPath?id=${encodeQueryValue(deviceId)}"

/**
 * 기기와 브라우저 샌드박스 안에서는 프로세스를 띄울 수 없어서, 같은 머신의 데스크탑 앱을 거치는
 * 것 말고는 SDK 도구에 닿을 방법이 없다.
 *
 * 타깃마다 HTTP 클라이언트가 달라서 바이트를 주고받는 두 함수만 밖에서 받는다. 경로를 조립하고
 * 본문을 해석하는 일은 여기서 한 번만 한다.
 *
 * [fetch] 가 null 을 주면(에이전트가 없거나 응답이 망가졌으면) 개수는 "셀 수 없음", 목록은 빈 목록,
 * 프레임은 없는 것이 된다.
 */
internal class HostAgentClient(
    private val fetch: suspend (path: String) -> ByteArray?,
    private val send: suspend (path: String, body: String) -> Unit,
) {
    suspend fun status(): EmulatorStatus? =
        fetch(HostAgentPath)?.decodeToString()?.let(::decodeEmulatorStatus)

    suspend fun devices(): List<EmulatorDevice>? =
        fetch(HostAgentDevicesPath)?.decodeToString()?.let(::decodeEmulatorDevices)

    suspend fun screen(deviceId: String): ByteArray? = fetch(hostAgentScreenPath(deviceId))

    suspend fun gesture(deviceId: String, gesture: EmulatorGesture) {
        send(HostAgentGesturePath, encodeEmulatorGesture(deviceId, gesture))
    }
}

internal fun hostAgentEmulatorDataSource(client: HostAgentClient): EmulatorDataSource =
    object : EmulatorDataSource {
        // HTTP 는 한 방향이라 에이전트가 변경을 알려줄 수 없다. 그래서 폴링으로 본다.
        override fun observeStatus(): Flow<EmulatorStatus> =
            observeByPolling(interval = HostAgentPollInterval) { client.status() ?: EmulatorStatus() }

        override fun observeDevices(): Flow<List<EmulatorDevice>> =
            observeByPolling(interval = HostAgentPollInterval) { client.devices().orEmpty() }

        override fun observeScreen(deviceId: String): Flow<ByteArray?> =
            observeByPolling(interval = EmulatorScreenPollInterval) { client.screen(deviceId) }

        override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
            client.gesture(deviceId, gesture)
        }
    }

// 에뮬레이터를 켜고 끄는 건 사람의 손이라 이보다 촘촘히 볼 이유가 없다.
private val HostAgentPollInterval = 5.seconds

/**
 * 기기 식별자를 질의 문자열에 싣는다. `avd:<이름>` 처럼 콜론이 들어가고 AVD 이름에 무엇이 올지
 * 모르므로 직접 인코딩한다. commonMain 에는 URL 인코더가 없다.
 */
private fun encodeQueryValue(value: String): String =
    value.encodeToByteArray().joinToString("") { byte ->
        val code = byte.toInt() and 0xFF
        val char = code.toChar()

        if (char in Unreserved) char.toString() else "%${code.toString(16).uppercase().padStart(2, '0')}"
    }

private val Unreserved = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('-', '_', '.', '~')
