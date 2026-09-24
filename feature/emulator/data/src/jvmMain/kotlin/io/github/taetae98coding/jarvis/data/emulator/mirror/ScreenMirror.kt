package io.github.taetae98coding.jarvis.data.emulator.mirror

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * adb 시리얼로 붙은 기기(에뮬레이터·실물 Android)의 화면을 scrcpy 영상 스트림으로 받고, 제스처를 같은
 * 세션의 제어 소켓으로 넣는다. 기기마다 세션 하나를 여럿이 나눠 쓴다(docs/common/device-mirroring.html).
 */
internal class ScreenMirror(
    private val sdk: File,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = ConcurrentHashMap<String, Session>()

    /** 이 기기의 프레임. 같은 기기를 여러 곳이 보면 스트림 하나를 replay 로 나눈다. */
    fun observeScreen(serial: String): Flow<EmulatorFrame?> = session(serial).frames

    /** 살아 있는 세션의 제어 소켓으로 이벤트를 보낸다. 아무도 화면을 보고 있지 않으면 조용히 버린다. */
    suspend fun sendGesture(serial: String, gesture: EmulatorGesture) {
        val connection = sessions[serial]?.connection?.get() ?: return
        withContext(Dispatchers.IO) {
            runCatching { connection.send(ControlMessages.encode(gesture)) }
        }
    }

    /**
     * 제어 소켓에 메시지 하나를 쓴다. [sendGesture] 와 달리 세션이 막 올라오는 중이면 [timeout] 까지 기다린다 — 도구는
     * 화면을 보는 구독(MCP 의 기기 임대)을 막 시작한 참일 수 있다. 끝내 연결이 없으면 false 다.
     */
    suspend fun send(serial: String, message: ByteArray, timeout: Duration = MirrorConnectTimeout): Boolean {
        val deadline = TimeSource.Monotonic.markNow() + timeout
        while (true) {
            val connection = sessions[serial]?.connection?.get()
            if (connection != null) {
                return withContext(Dispatchers.IO) { runCatching { connection.send(message) }.isSuccess }
            }
            if (deadline.hasPassedNow()) return false
            delay(ConnectionPollInterval)
        }
    }

    private fun session(serial: String): Session = sessions.getOrPut(serial) { Session(serial) }

    private inner class Session(
        private val serial: String,
    ) {
        val connection = AtomicReference<ScrcpyServer.Connection?>(null)

        val frames: Flow<EmulatorFrame?> =
            channelFlow {
                // 소켓 읽기는 블로킹이라 코루틴 취소로 멈추지 않는다. awaitClose 에서 소켓을 닫아 깨운다.
                val worker = launch(Dispatchers.IO) { stream() }
                awaitClose {
                    connection.getAndSet(null)?.close()
                    worker.cancel()
                }
            }
                // 느린 구독자가 프레임을 밀어 두지 않는다. 픽셀 버퍼 규약상 최신 한 장만 든다.
                .buffer(Channel.CONFLATED)
                .shareIn(
                    scope,
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = MirrorSessionLinger.inWholeMilliseconds,
                        replayExpirationMillis = 0,
                    ),
                    replay = 1,
                )

        private suspend fun kotlinx.coroutines.channels.ProducerScope<EmulatorFrame?>.stream() {
            while (isActive) {
                var current: ScrcpyServer.Connection? = null
                try {
                    val connected = ScrcpyServer.start(sdk, serial)
                    current = connected
                    connection.set(connected)

                    H264Decoder().use { decoder ->
                        while (isActive) {
                            val element = connected.video.readNext()
                            if (element is ScrcpyStream.Element.Packet) {
                                decoder.decode(element.payload, element.payload.size)?.let { trySend(it) }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 올리지 못했거나 스트림이 끊겼다. 마지막 프레임은 화면이 들고 있으므로 null 을 한 번
                    // 보내 "가져오는 중" 과 "가져올 수 없음" 을 가르고, 잠시 뒤 처음부터 다시 한다.
                    trySend(null)
                    delay(MirrorRetryDelay)
                } finally {
                    connection.set(null)
                    current?.close()
                }
            }
        }
    }
}

private val ConnectionPollInterval = 50.milliseconds
