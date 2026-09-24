package io.github.taetae98coding.jarvis.data.emulator.agent

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.js.ExperimentalWasmJsInterop

internal val webHostAgentClient = HostAgentClient(
    fetch = ::fetchFromHostAgent,
    send = ::sendToHostAgent,
    exchange = ::exchangeWithHostAgent,
)

// 브라우저에서 localhost 는 페이지를 띄운 머신이고, 개발 서버와 에이전트가 같은 머신에 있으면
// 그게 곧 개발자 머신이다. 포트가 달라 교차 출처가 되므로 에이전트가 CORS 헤더를 붙인다.
private const val Loopback = "localhost"

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun fetchFromHostAgent(path: String): ByteArray? =
    // 에이전트가 없으면 fetch 가 TypeError 로 거절된다. 연결 실패와 잘못된 응답을 가릴 필요가 없어서
    // 둘 다 null 로 묶는다.
    runCatching {
        // await 의 타입 인자를 적어 준다. Promise 가 out 변성이라 반환 타입만으로는 추론되지 않는다.
        val response = window.fetch(hostAgentUrl(Loopback, path)).await<Response>()

        if (response.ok) response.arrayBuffer().await<ArrayBuffer>().toByteArray() else null
    }.getOrNull()

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun sendToHostAgent(path: String, body: String) {
    runCatching {
        // 본문이 문자열이면 브라우저가 Content-Type 을 text/plain 으로 붙인다. 그래야 단순 요청이
        // 되어 CORS 사전 요청(OPTIONS) 왕복이 한 번 줄어든다.
        window.fetch(hostAgentUrl(Loopback, path), RequestInit(method = "POST", body = body.toJsString()))
            .await<Response>()
    }
}

// fetch 에는 기본 타임아웃이 없어서 에이전트가 `adb pair` 를 끝낼 때까지 기다린다.
@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun exchangeWithHostAgent(path: String, body: String): ByteArray? =
    runCatching {
        val response = window.fetch(hostAgentUrl(Loopback, path), RequestInit(method = "POST", body = body.toJsString()))
            .await<Response>()

        if (response.ok) response.arrayBuffer().await<ArrayBuffer>().toByteArray() else null
    }.getOrNull()

// 프레임은 JSON 이 아니라 PNG 바이트다. ArrayBuffer 를 Kotlin 쪽으로 한 번 복사해야 한다.
@OptIn(ExperimentalWasmJsInterop::class)
private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)

    return ByteArray(view.length) { view[it] }
}
