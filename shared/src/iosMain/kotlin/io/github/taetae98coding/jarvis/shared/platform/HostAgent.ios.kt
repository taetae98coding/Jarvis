package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithURL
import kotlin.coroutines.resume

// 시뮬레이터에서 127.0.0.1 은 시뮬레이터를 띄운 Mac 이다. 실물 기기에서는 자기 자신이라 에이전트가
// 없고, 연결이 거절되어 "셀 수 없음" 으로 떨어진다.
private const val Loopback = "127.0.0.1"

internal suspend fun fetchHostAgentEmulators(): EmulatorStatus? =
    suspendCancellableCoroutine { continuation ->
        val url = NSURL.URLWithString(hostAgentUrl(Loopback))

        if (url == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val task = NSURLSession.sharedSession.dataTaskWithURL(url) { data, response, _ ->
            val ok = (response as? NSHTTPURLResponse)?.statusCode == 200L
            val body = if (ok) data?.decodeToStringOrNull() else null

            continuation.resume(body?.let(::decodeEmulatorStatus))
        }

        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }

// NSString 으로 감싸 String 에 캐스팅하는 관용구는 정적 타입이 이어지지 않아 경고가 난다. 바이트를
// 그대로 읽어 UTF-8 로 디코딩하면 타입이 분명하고, 깨진 바이트는 대체 문자가 되어 파싱에서 걸린다.
@OptIn(ExperimentalForeignApi::class)
private fun NSData.decodeToStringOrNull(): String? =
    bytes?.readBytes(length.toInt())?.decodeToString()
