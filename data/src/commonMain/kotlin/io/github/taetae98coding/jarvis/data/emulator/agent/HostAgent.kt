package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.data.emulator.EmulatorDataSource
import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlin.time.Duration.Companion.seconds

/**
 * 데스크탑 앱이 띄우는 로컬 HTTP 에이전트의 주소.
 *
 * 등록되지 않은 대역에서 고정 포트를 하나 골랐다. 클라이언트가 포트를 미리 알아야 해서 OS 가
 * 골라주는 임의 포트를 쓸 수 없다.
 */
internal const val HostAgentPort: Int = 47890
internal const val HostAgentPath: String = "/emulators"

internal fun hostAgentUrl(host: String): String = "http://$host:$HostAgentPort$HostAgentPath"

/**
 * 기기와 브라우저 샌드박스 안에서는 프로세스를 띄울 수 없어서, 같은 머신의 데스크탑 앱을 거치는
 * 것 말고는 SDK 도구에 닿을 방법이 없다. 타깃마다 HTTP 클라이언트가 달라 [fetch] 로 받는다.
 *
 * [fetch] 가 null 을 주면(에이전트가 없거나 응답이 망가졌으면) 개수를 셀 수 없는 상태가 된다.
 */
internal fun hostAgentEmulatorDataSource(fetch: suspend () -> EmulatorStatus?): EmulatorDataSource =
    EmulatorDataSource {
        // HTTP 는 한 방향이라 에이전트가 변경을 알려줄 수 없다. 그래서 폴링으로 본다.
        observeByPolling(interval = HostAgentPollInterval) { fetch() ?: EmulatorStatus() }
    }

// 에뮬레이터를 켜고 끄는 건 사람의 손이라 이보다 촘촘히 볼 이유가 없다.
private val HostAgentPollInterval = 5.seconds
