package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HostAgentTest {
    @Test
    fun countsSurviveTheWireFormat() {
        val status = EmulatorStatus(
            android = EmulatorSummary(total = 5, running = 2, physical = 1),
            // 셀 수 없는 종류는 0개가 아니라 없음으로 전달되어야 한다.
            ios = null,
        )

        assertEquals(status, decodeEmulatorStatus(encodeEmulatorStatus(status)))
    }

    @Test
    fun brokenResponsesAreNotCounts() {
        assertNull(decodeEmulatorStatus("<html>Proxy Error</html>"))
    }

    @Test
    fun newerAgentFieldsAreIgnored() {
        val body = """{"android":{"total":1,"running":1},"web":{"total":9}}"""

        assertEquals(EmulatorStatus(android = EmulatorSummary(total = 1, running = 1)), decodeEmulatorStatus(body))
    }

    @Test
    fun devicesSurviveTheWireFormat() {
        val devices = listOf(RunningDevice, StoppedDevice, PhysicalDevice)

        assertEquals(devices, decodeEmulatorDevices(encodeEmulatorDevices(devices)))
    }

    // 모르는 플랫폼 이름을 그대로 태우면 화면에 정체를 알 수 없는 줄이 남는다.
    @Test
    fun devicesOfUnknownPlatformsAreDropped() {
        val body = """{"devices":[{"id":"1","name":"Watch","platform":"tizen"}]}"""

        assertEquals(emptyList(), decodeEmulatorDevices(body))
    }

    @Test
    fun brokenResponsesAreNotDeviceLists() {
        assertNull(decodeEmulatorDevices("<html>Proxy Error</html>"))
    }

    @Test
    fun gesturesSurviveTheWireFormat() {
        val swipe = EmulatorGesture.Swipe(fromX = 1, fromY = 2, toX = 3, toY = 4, durationMillis = 120)
        val decoded = decodeEmulatorGesture(encodeEmulatorGesture(RunningDevice.id, swipe))

        assertEquals(RunningDevice.id, decoded?.deviceId)
        assertEquals(swipe, decoded?.gesture)
    }

    @Test
    fun unknownGestureTypesAreNotGestures() {
        assertNull(decodeEmulatorGesture("""{"id":"emulator-5554","type":"pinch"}"""))
    }

    @Test
    fun missingAgentMeansUncountable() = runTest {
        val statuses = silentAgent().observeStatus().take(1).toList()

        assertEquals(listOf(EmulatorStatus()), statuses)
    }

    @Test
    fun missingAgentMeansNoDevices() = runTest {
        val devices = silentAgent().observeDevices().take(1).toList()

        assertEquals(listOf(emptyList()), devices)
    }

    @Test
    fun missingAgentMeansNoFrames() = runTest {
        val frames = silentAgent().observeScreen(RunningDevice.id).take(1).toList()

        assertEquals(listOf(null), frames)
    }

    @Test
    fun screenRequestsCarryTheDeviceIdentifier() = runTest {
        val paths = mutableListOf<String>()
        val client = HostAgentClient(fetch = { path -> paths += path; null }, send = { _, _ -> })

        hostAgentEmulatorDataSource(client).observeScreen(StoppedDevice.id).take(1).toList()

        // 콜론이 살아 있어야 에이전트가 어느 기기인지 알아본다.
        assertEquals(listOf("/emulators/screen?id=avd%3APixel_Tablet_API_36"), paths)
    }

    @Test
    fun framesArePassedThroughAsBytes() = runTest {
        val frame = byteArrayOf(0x89.toByte(), 'P'.code.toByte())
        val client = HostAgentClient(fetch = { frame }, send = { _, _ -> })

        val frames = hostAgentEmulatorDataSource(client).observeScreen(RunningDevice.id).take(1).toList()

        assertContentEquals(frame, frames.single())
    }

    @Test
    fun gesturesAreSentToTheAgent() = runTest {
        val sent = mutableListOf<Pair<String, String>>()
        val client = HostAgentClient(fetch = { null }, send = { path, body -> sent += path to body })

        hostAgentEmulatorDataSource(client).sendGesture(RunningDevice.id, EmulatorGesture.Tap(x = 7, y = 9))

        assertEquals(HostAgentGesturePath, sent.single().first)
        assertEquals(
            RunningDevice.id to EmulatorGesture.Tap(x = 7, y = 9),
            decodeEmulatorGesture(sent.single().second)?.let { it.deviceId to it.gesture },
        )
    }

    @Test
    fun launchRequestsAreSentToTheAgent() = runTest {
        val sent = mutableListOf<Pair<String, String>>()
        val client = HostAgentClient(fetch = { null }, send = { path, body -> sent += path to body })

        hostAgentEmulatorDataSource(client).launch(StoppedDevice.id)

        assertEquals(HostAgentLaunchPath, sent.single().first)
        assertEquals(StoppedDevice.id, decodeEmulatorDeviceId(sent.single().second))
    }

    @Test
    fun wakeRequestsAreSentToTheAgent() = runTest {
        val sent = mutableListOf<Pair<String, String>>()
        val client = HostAgentClient(fetch = { null }, send = { path, body -> sent += path to body })

        hostAgentEmulatorDataSource(client).wake(RunningDevice.id)

        assertEquals(HostAgentWakePath, sent.single().first)
        assertEquals(RunningDevice.id, decodeEmulatorDeviceId(sent.single().second))
    }

    @Test
    fun brokenBodiesAreNotLaunchRequests() {
        assertNull(decodeEmulatorDeviceId("not json"))
    }

    // 옛 에이전트는 능력 플래그를 모른다. 할 수 없는 일을 할 수 있다고 읽으면 요청이 기기까지 간다.
    @Test
    fun devicesFromAnOlderAgentCanDoNothing() {
        val body = """{"devices":[{"id":"emulator-5554","name":"Pixel","platform":"android","running":true}]}"""

        val device = decodeEmulatorDevices(body)?.single()

        assertEquals(false, device?.isAsleep)
        assertEquals(false, device?.canStream)
        assertEquals(false, device?.canControl)
        assertEquals(false, device?.canLaunch)
    }

    @Test
    fun dataSourceFollowsAgentUpdates() = runTest {
        var answer: EmulatorStatus? = null
        val statuses = mutableListOf<EmulatorStatus>()
        val client = HostAgentClient(
            fetch = { answer?.let { encodeEmulatorStatus(it).encodeToByteArray() } },
            send = { _, _ -> },
        )
        backgroundScope.launch { hostAgentEmulatorDataSource(client).observeStatus().toList(statuses) }
        runCurrent()

        answer = EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))
        advanceTimeBy(6.seconds)

        assertEquals(
            listOf(EmulatorStatus(), EmulatorStatus(android = EmulatorSummary(total = 3, running = 1))),
            statuses,
        )
    }

    @Test
    fun pairingServicesSurviveTheWireFormat() {
        val services = listOf(WaitingService, WaitingService.copy(name = "adb-R3CY705Y62R-WbpIT6 (2)", port = 40001))

        assertEquals(services, decodePairingServices(encodePairingServices(services)))
    }

    @Test
    fun pairRequestsCarryTheServiceAndCode() = runTest {
        val sent = mutableListOf<Pair<String, String>>()
        val client = HostAgentClient(
            fetch = { null },
            send = { _, _ -> },
            exchange = { path, body ->
                sent += path to body
                encodePairResult(PairingResult.Paired(isConnected = false)).encodeToByteArray()
            },
        )

        val result = hostAgentDevicePairingDataSource(client).pair(WaitingService, "012345")

        assertEquals(PairingResult.Paired(isConnected = false), result)
        assertEquals(HostAgentPairPath, sent.single().first)
        val request = decodePairRequest(sent.single().second)
        assertEquals(WaitingService, request?.service)
        assertEquals("012345", request?.code)
    }

    @Test
    fun pairFailuresKeepTheReason() {
        val failure = PairingResult.Failed("Failed: Wrong password or connection was dropped.")

        assertEquals(failure, decodePairResult(encodePairResult(failure)))
    }

    @Test
    fun missingAgentMeansPairingServicesCannotBeFound() = runTest {
        val services = silentPairing().observePairingServices().take(1).toList()

        assertEquals(listOf(null), services)
    }

    @Test
    fun missingAgentMeansPairingFailed() = runTest {
        assertIs<PairingResult.Failed>(silentPairing().pair(WaitingService, "012345"))
    }

    private fun silentPairing() =
        hostAgentDevicePairingDataSource(HostAgentClient(fetch = { null }, send = { _, _ -> }, exchange = { _, _ -> null }))

    // 에이전트가 없을 때의 클라이언트. 모든 요청이 아무것도 돌려주지 않는다.
    private fun silentAgent() =
        hostAgentEmulatorDataSource(HostAgentClient(fetch = { null }, send = { _, _ -> }))

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

        val PhysicalDevice = EmulatorDevice(
            id = "ios:00008130-000A1C2E0298001C",
            name = "Jarvis의 iPhone",
            platform = EmulatorPlatform.IOS,
            isPhysical = true,
            isRunning = true,
        )
    }
}
