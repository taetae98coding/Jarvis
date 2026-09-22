package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Response
import kotlin.js.ExperimentalWasmJsInterop

// 브라우저에서 localhost 는 페이지를 띄운 머신이고, 개발 서버와 에이전트가 같은 머신에 있으면
// 그게 곧 개발자 머신이다. 포트가 달라 교차 출처가 되므로 에이전트가 CORS 헤더를 붙인다.
private const val Loopback = "localhost"

@OptIn(ExperimentalWasmJsInterop::class)
internal suspend fun fetchHostAgentEmulators(): EmulatorStatus? =
    // 에이전트가 없으면 fetch 가 TypeError 로 거절된다. 연결 실패와 잘못된 응답을 가릴 필요가 없어서
    // 둘 다 null 로 묶는다.
    runCatching {
        // await 의 타입 인자를 적어 준다. Promise 가 out 변성이라 반환 타입만으로는 추론되지 않는다.
        val response = window.fetch(hostAgentUrl(Loopback)).await<Response>()

        if (response.ok) response.text().await<JsString>().toString() else null
    }.getOrNull()
        ?.let(::decodeEmulatorStatus)
