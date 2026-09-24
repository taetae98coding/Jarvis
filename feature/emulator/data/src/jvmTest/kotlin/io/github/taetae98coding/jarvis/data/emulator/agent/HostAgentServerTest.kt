package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.data.emulator.DevicePairingDataSource
import io.github.taetae98coding.jarvis.data.emulator.EmulatorDataSource
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class HostAgentServerTest {
    @Test
    fun servesTheLatestCountOverHttp() {
        val status = EmulatorStatus(android = EmulatorSummary(total = 5, running = 2), ios = null)

        withAgent(FakeEmulatorDataSource(statuses = MutableStateFlow(status))) { port ->
            assertEquals(status, decodeEmulatorStatus(awaitBody(port, HostAgentPath)))
        }
    }

    @Test
    fun answersServiceUnavailableBeforeTheFirstCount() {
        // 아직 세지 못한 상태를 0개로 답하면 클라이언트가 "셀 수 없음" 과 구분할 수 없다.
        withAgent(FakeEmulatorDataSource()) { port ->
            assertEquals(503, request(port, HostAgentPath).code)
        }
    }

    @Test
    fun rejectsOtherMethods() {
        withAgent(FakeEmulatorDataSource(statuses = MutableStateFlow(EmulatorStatus()))) { port ->
            assertEquals(405, request(port, HostAgentPath, method = "POST").code)
        }
    }

    @Test
    fun letsBrowserOriginsReadTheResponse() {
        // webApp 은 다른 포트에서 서빙되므로 CORS 헤더가 없으면 브라우저가 응답을 읽지 못한다.
        withAgent(FakeEmulatorDataSource(statuses = MutableStateFlow(EmulatorStatus()))) { port ->
            awaitBody(port, HostAgentPath)

            assertEquals("*", request(port, HostAgentPath).corsOrigin)
        }
    }

    @Test
    fun staysSilentWhenThePortIsTaken() {
        val port = freePort()
        val dataSource = FakeEmulatorDataSource(statuses = MutableStateFlow(EmulatorStatus()))

        startEmulatorHostAgent(port, dataSource, FakeDevicePairingDataSource()).use {
            // 두 번째 에이전트는 예외를 던지지 않고 아무것도 하지 않는다.
            startEmulatorHostAgent(port, dataSource, FakeDevicePairingDataSource()).close()

            assertEquals(200, request(port, HostAgentPath).code)
        }
    }

    @Test
    fun servesTheDeviceList() {
        val devices = listOf(RunningDevice, StoppedDevice)

        withAgent(FakeEmulatorDataSource(devices = MutableStateFlow(devices))) { port ->
            assertEquals(devices, decodeEmulatorDevices(awaitBody(port, HostAgentDevicesPath)))
        }
    }

    @Test
    fun servesAFrameAsPng() {
        val frame = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())

        withAgent(FakeEmulatorDataSource(frames = mapOf(RunningDevice.id to EmulatorFrame.Encoded(frame)))) { port ->
            val response = request(port, hostAgentScreenPath(RunningDevice.id))

            assertEquals(200, response.code)
            assertEquals("image/png", response.contentType)
            assertContentEquals(frame, response.bytes)
        }
    }

    // 스트림에서 온 디코딩된 픽셀은 JPEG 로 인코딩해 보낸다.
    @Test
    fun encodesPixelFramesAsJpeg() {
        val pixels = EmulatorFrame.Pixels(width = 2, height = 2, pixels = ByteArray(2 * 2 * 4) { 0xFF.toByte() })

        withAgent(FakeEmulatorDataSource(frames = mapOf(RunningDevice.id to pixels))) { port ->
            val response = request(port, hostAgentScreenPath(RunningDevice.id))

            assertEquals(200, response.code)
            assertEquals("image/jpeg", response.contentType)
            // JPEG 시그니처(FF D8).
            assertEquals(0xFF.toByte(), response.bytes!![0])
            assertEquals(0xD8.toByte(), response.bytes!![1])
        }
    }

    // 꺼져 있는 기기는 찍을 화면이 없다. 빈 본문을 200 으로 주면 클라이언트가 깨진 PNG 로 받는다.
    @Test
    fun answersServiceUnavailableWhenThereIsNoFrame() {
        withAgent(FakeEmulatorDataSource()) { port ->
            assertEquals(503, request(port, hostAgentScreenPath(StoppedDevice.id)).code)
        }
    }

    // 꺼진 AVD 의 id 에는 콜론이 들어간다. 질의 문자열에서 되살아나야 기기를 찾을 수 있다.
    @Test
    fun keepsIdentifiersIntactThroughTheQueryString() {
        val frame = byteArrayOf(1, 2, 3)

        withAgent(FakeEmulatorDataSource(frames = mapOf(StoppedDevice.id to EmulatorFrame.Encoded(frame)))) { port ->
            assertContentEquals(frame, request(port, hostAgentScreenPath(StoppedDevice.id)).bytes)
        }
    }

    @Test
    fun forwardsGestures() {
        val dataSource = FakeEmulatorDataSource()

        withAgent(dataSource) { port ->
            val gesture: EmulatorGesture =
                EmulatorGesture.Touch(action = TouchAction.MOVE, x = 1, y = 2, frameWidth = 1080, frameHeight = 2400)
            val response = request(
                port = port,
                path = HostAgentGesturePath,
                method = "POST",
                body = encodeEmulatorGesture(RunningDevice.id, gesture),
            )

            assertEquals(204, response.code)
            assertEquals(listOf(RunningDevice.id to gesture), dataSource.gestures)
        }
    }

    @Test
    fun rejectsGesturesItCannotRead() {
        val dataSource = FakeEmulatorDataSource()

        withAgent(dataSource) { port ->
            val response = request(port, HostAgentGesturePath, method = "POST", body = "not json")

            assertEquals(400, response.code)
            assertTrue(dataSource.gestures.isEmpty())
        }
    }

    @Test
    fun forwardsLaunchRequests() {
        val dataSource = FakeEmulatorDataSource()

        withAgent(dataSource) { port ->
            val response = request(
                port = port,
                path = HostAgentLaunchPath,
                method = "POST",
                body = encodeEmulatorDeviceId(StoppedDevice.id),
            )

            assertEquals(204, response.code)
            assertEquals(listOf(StoppedDevice.id), dataSource.launched)
        }
    }

    @Test
    fun rejectsLaunchRequestsItCannotRead() {
        val dataSource = FakeEmulatorDataSource()

        withAgent(dataSource) { port ->
            val response = request(port, HostAgentLaunchPath, method = "POST", body = "not json")

            assertEquals(400, response.code)
            assertTrue(dataSource.launched.isEmpty())
        }
    }

    @Test
    fun forwardsWakeRequests() {
        val dataSource = FakeEmulatorDataSource()

        withAgent(dataSource) { port ->
            val response = request(
                port = port,
                path = HostAgentWakePath,
                method = "POST",
                body = encodeEmulatorDeviceId(RunningDevice.id),
            )

            assertEquals(204, response.code)
            assertEquals(listOf(RunningDevice.id), dataSource.woken)
        }
    }

    @Test
    fun servesPairingServices() {
        val services = listOf(WaitingService)

        withAgent(pairing = FakeDevicePairingDataSource(services = services)) { port ->
            assertEquals(services, decodePairingServices(awaitBody(port, HostAgentPairingServicesPath)))
        }
    }

    // 빈 목록으로 답하면 "기다리는 기기가 없다" 와 "찾을 방법이 없다" 가 섞인다.
    @Test
    fun answersServiceUnavailableWhenPairingServicesCannotBeFound() {
        withAgent(pairing = FakeDevicePairingDataSource(services = null)) { port ->
            assertEquals(503, request(port, HostAgentPairingServicesPath).code)
        }
    }

    @Test
    fun answersWithThePairingResult() {
        val failure = PairingResult.Failed("Failed: Wrong password or connection was dropped.")
        val pairing = FakeDevicePairingDataSource(result = failure)

        withAgent(pairing = pairing) { port ->
            val response = request(
                port = port,
                path = HostAgentPairPath,
                method = "POST",
                body = encodePairRequest(WaitingService, "123456"),
            )

            assertEquals(200, response.code)
            assertEquals(failure, response.bytes?.decodeToString()?.let(::decodePairResult))
            assertEquals(listOf(WaitingService to "123456"), pairing.paired)
        }
    }

    @Test
    fun rejectsBrokenPairRequests() {
        val pairing = FakeDevicePairingDataSource()

        withAgent(pairing = pairing) { port ->
            assertEquals(400, request(port, HostAgentPairPath, method = "POST", body = "not json").code)
            assertTrue(pairing.paired.isEmpty())
        }
    }

    private fun withAgent(
        dataSource: EmulatorDataSource = FakeEmulatorDataSource(),
        pairing: DevicePairingDataSource = FakeDevicePairingDataSource(),
        block: (Int) -> Unit,
    ) {
        val port = freePort()

        startEmulatorHostAgent(port, dataSource, pairing).use { block(port) }
    }

    // 에이전트는 개수와 목록을 백그라운드에서 받아 두므로 첫 요청이 503 일 수 있다.
    private fun awaitBody(port: Int, path: String): String {
        repeat(20) {
            val response = request(port, path)

            if (response.code == 200 && response.bytes != null) return response.bytes.decodeToString()

            Thread.sleep(50)
        }

        fail("에이전트가 $path 에 응답하지 않았다")
    }

    private fun request(
        port: Int,
        path: String,
        method: String = "GET",
        body: String? = null,
    ): AgentResponse {
        val url = URI("http://127.0.0.1:$port$path").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doOutput = body != null
        }

        try {
            body?.let { connection.outputStream.use { stream -> stream.write(it.encodeToByteArray()) } }

            val code = connection.responseCode

            return AgentResponse(
                code = code,
                bytes = if (code == 200) connection.inputStream.use(InputStream::readBytes) else null,
                corsOrigin = connection.getHeaderField("Access-Control-Allow-Origin"),
                contentType = connection.getHeaderField("Content-Type"),
            )
        } finally {
            connection.disconnect()
        }
    }

    // 에이전트를 띄우기 직전에 포트를 놓아주는 방식이라 이론적으로는 경합이 있다. 고정 포트를 쓰면
    // 개발자 머신에서 실제로 도는 에이전트와 부딪히므로 이쪽을 택했다.
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private class AgentResponse(
        val code: Int,
        val bytes: ByteArray?,
        val corsOrigin: String?,
        val contentType: String?,
    )

    private class FakeEmulatorDataSource(
        private val statuses: Flow<EmulatorStatus> = emptyFlow(),
        private val devices: Flow<List<EmulatorDevice>> = emptyFlow(),
        private val frames: Map<String, EmulatorFrame> = emptyMap(),
    ) : EmulatorDataSource {
        val gestures = mutableListOf<Pair<String, EmulatorGesture>>()

        val launched = mutableListOf<String>()

        val woken = mutableListOf<String>()

        override fun observeStatus() = statuses

        override fun observeDevices() = devices

        override fun observeScreen(deviceId: String) = flowOf(frames[deviceId])

        override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
            gestures += deviceId to gesture
        }

        override suspend fun launch(deviceId: String) {
            launched += deviceId
        }

        override suspend fun wake(deviceId: String) {
            woken += deviceId
        }
    }

    private class FakeDevicePairingDataSource(
        private val services: List<PairingService>? = emptyList(),
        private val result: PairingResult = PairingResult.Paired(isConnected = true),
    ) : DevicePairingDataSource {
        val paired = mutableListOf<Pair<PairingService, String>>()

        override fun observePairingServices() = flowOf(services)

        override suspend fun pair(service: PairingService, code: String): PairingResult {
            paired += service to code
            return result
        }
    }

    private companion object {
        val WaitingService = PairingService(name = "adb-R54T202XEHN-Y2yH0N", host = "172.30.1.47", port = 37123)

        val RunningDevice = EmulatorDevice(
            id = "emulator-5554",
            name = "Pixel_9_API_37",
            platform = EmulatorPlatform.ANDROID,
            isRunning = true,
            canStream = true,
            canControl = true,
        )

        val StoppedDevice = EmulatorDevice(
            id = "avd:Pixel_Tablet_API_36",
            name = "Pixel_Tablet_API_36",
            platform = EmulatorPlatform.ANDROID,
            canLaunch = true,
        )
    }
}
