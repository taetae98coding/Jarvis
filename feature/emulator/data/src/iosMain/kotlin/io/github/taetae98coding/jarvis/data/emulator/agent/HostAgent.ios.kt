package io.github.taetae98coding.jarvis.data.emulator.agent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataTaskWithURL
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.coroutines.resume

internal val iosHostAgentClient = HostAgentClient(
    fetch = ::fetchFromHostAgent,
    send = ::sendToHostAgent,
)

// 시뮬레이터에서 127.0.0.1 은 시뮬레이터를 띄운 Mac 이다. 실물 기기에서는 자기 자신이라 에이전트가
// 없고, 연결이 거절되어 빈 목록으로 떨어진다.
private const val Loopback = "127.0.0.1"

private suspend fun fetchFromHostAgent(path: String): ByteArray? =
    suspendCancellableCoroutine { continuation ->
        val url = NSURL.URLWithString(hostAgentUrl(Loopback, path))

        if (url == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val task = NSURLSession.sharedSession.dataTaskWithURL(url) { data, response, _ ->
            val ok = (response as? NSHTTPURLResponse)?.statusCode == 200L

            continuation.resume(if (ok) data?.toByteArray() else null)
        }

        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }

private suspend fun sendToHostAgent(path: String, body: String) {
    suspendCancellableCoroutine { continuation ->
        val url = NSURL.URLWithString(hostAgentUrl(Loopback, path))

        if (url == null) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val request = NSMutableURLRequest.requestWithURL(url).apply {
            setHTTPMethod("POST")
            // 브라우저 클라이언트가 CORS 사전 요청을 피하려고 쓰는 타입에 맞춘다.
            setValue("text/plain", forHTTPHeaderField = "Content-Type")
            setHTTPBody(body.encodeToByteArray().toNSData())
        }

        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { _, _, _ ->
            // 제스처는 결과를 되돌려 받지 않는다. 실패하면 사용자가 한 번 더 누른다.
            continuation.resume(Unit)
        }

        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }
}

// NSString 으로 감싸 String 에 캐스팅하는 관용구는 정적 타입이 이어지지 않아 경고가 난다. 바이트를
// 그대로 읽으면 타입이 분명하고, JSON 이 깨져 있으면 파싱에서 걸린다.
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray? = bytes?.readBytes(length.toInt())

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
