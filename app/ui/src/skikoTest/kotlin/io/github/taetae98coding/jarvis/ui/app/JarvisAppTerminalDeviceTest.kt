package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorFrameTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewDeviceTabEmptyTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalNewShellTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalDeviceTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalNewDeviceTabTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** docs/common/terminal-device.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalDeviceTest {
    private fun ComposeUiTest.openTerminal(
        emulator: FakeEmulatorRepository,
        terminal: FakeTerminalRepository = FakeTerminalRepository(),
        workspace: FakeTerminalWorkspaceRepository = FakeTerminalWorkspaceRepository(),
    ) {
        setContent { TestJarvisApp(emulator = emulator, terminal = terminal, terminalWorkspace = workspace) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
    }

    /** + 를 눌러 [device] 줄을 고르고, 열린 기기 탭의 id 를 돌려준다. */
    private fun ComposeUiTest.openDeviceTab(workspace: FakeTerminalWorkspaceRepository, deviceId: String): Long {
        onNode(newTabButton).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(terminalNewDeviceTabTestTag(deviceId)).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(terminalNewDeviceTabTestTag(deviceId)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.program == TerminalProgram.Device }

        return workspace.workspace.value.focusedTab!!.id
    }

    private fun ComposeUiTest.awaitFrame(tabId: Long) {
        val frame = hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId)))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodes(frame).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun streaming(vararg devices: EmulatorDevice) =
        FakeEmulatorRepository(devices = devices.toList(), frames = MutableStateFlow(TestFrame))

    @Test
    fun menuListsOnlyDevicesWhoseScreenCanBeSeen() = runComposeUiTest {
        openTerminal(streaming(RunningAndroidDevice, StoppedAndroidDevice, PhysicalAndroidDevice, SleepingAndroidDevice, RunningSimulator, PhysicalIosDevice))

        onNode(newTabButton).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(terminalNewDeviceTabTestTag(RunningAndroidDevice.id)).fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithText("기기").assertIsDisplayed()
        onNodeWithTag(terminalNewDeviceTabTestTag(RunningAndroidDevice.id)).assertIsDisplayed()
        onNodeWithText("Android 에뮬레이터").assertIsDisplayed()
        onNodeWithTag(terminalNewDeviceTabTestTag(PhysicalAndroidDevice.id)).assertIsDisplayed()
        onNodeWithTag(terminalNewDeviceTabTestTag(RunningSimulator.id)).assertIsDisplayed()
        // 실물 기기는 유선·무선을 함께 적는다.
        onNodeWithText("Android 실물 기기 · 유선").assertIsDisplayed()
        onNodeWithText("Android 실물 기기 · 무선").assertIsDisplayed()
        onNodeWithText("iOS 시뮬레이터").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(terminalNewDeviceTabTestTag(StoppedAndroidDevice.id)).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithTag(terminalNewDeviceTabTestTag(PhysicalIosDevice.id)).fetchSemanticsNodes().size)

        // 기기 구획은 셸·Claude 항목 아래다.
        val shell = onNodeWithTag(TerminalNewShellTabTestTag).fetchSemanticsNode().boundsInRoot.top
        val device = onNodeWithTag(terminalNewDeviceTabTestTag(RunningAndroidDevice.id)).fetchSemanticsNode().boundsInRoot.top
        assertTrue(shell < device)
    }

    @Test
    fun menuSaysSoWhenNoDeviceCanBeSeen() = runComposeUiTest {
        openTerminal(streaming(StoppedAndroidDevice, PhysicalIosDevice))

        onNode(newTabButton).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodes(hasText("화면을 볼 수 있는 기기가 없습니다")).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(TerminalNewDeviceTabEmptyTestTag).assertIsNotEnabled()
    }

    @Test
    fun menuSaysItIsLookingBeforeTheFirstList() = runComposeUiTest {
        setContent { TestJarvisApp(emulator = SilentEmulatorRepository) }
        onNodeWithTag(TerminalTestTag).performClick()

        onNode(newTabButton).performClick()

        onNodeWithText("기기를 찾는 중…").assertIsDisplayed()
        onNodeWithTag(TerminalNewDeviceTabEmptyTestTag).assertIsNotEnabled()
    }

    @Test
    fun pickingADeviceOpensItsScreenInANewTabNamedAfterIt() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository()
        val terminal = FakeTerminalRepository()
        openTerminal(streaming(RunningAndroidDevice), terminal, workspace)

        val tabId = openDeviceTab(workspace, RunningAndroidDevice.id)

        val tab = workspace.workspace.value.focusedTab!!
        assertEquals(RunningAndroidDevice.id, tab.deviceId)
        assertEquals(RunningAndroidDevice.name, tab.deviceName)
        assertEquals(2, workspace.workspace.value.groups.single().tabs.size)
        onNode(hasText(RunningAndroidDevice.name) and hasAnyAncestor(hasTestTag(terminalTabTestTag(tabId)))).assertIsDisplayed()
        awaitFrame(tabId)
        // 기기 탭은 셸을 띄우지 않는다.
        assertEquals(1, terminal.sessions.size)
    }

    // 실물 기기·에뮬레이터의 영상 스트림은 PNG 가 아니라 디코더가 푼 BGRA 픽셀로 온다. 폭과 높이가 다른
    // 프레임이라야 행 간격이 틀렸을 때 그리기가 실패한다.
    @Test
    fun videoFrameIsDrawnInTheDeviceTab() = runComposeUiTest {
        val workspace = FakeTerminalWorkspaceRepository()
        val pixels = EmulatorFrame.Pixels(width = 8, height = 16, pixels = ByteArray(8 * 16 * 4) { 0x40 })
        openTerminal(FakeEmulatorRepository(devices = listOf(PhysicalAndroidDevice), frames = MutableStateFlow(pixels)), workspace = workspace)

        val tabId = openDeviceTab(workspace, PhysicalAndroidDevice.id)
        awaitFrame(tabId)

        val frame = onNode(hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId))))
        frame.assertIsDisplayed()
        frame.captureToImage()
    }

    @Test
    fun tapOnTheDeviceTabIsSentInDeviceCoordinates() = runComposeUiTest {
        val emulator = streaming(RunningAndroidDevice)
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(emulator, workspace = workspace)
        val tabId = openDeviceTab(workspace, RunningAndroidDevice.id)
        awaitFrame(tabId)

        onNode(hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId))))
            .performTouchInput { click(center) }

        // 마우스 클릭은 앞서 호버가 끼기도 한다. 누름·뗌만 본다.
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            emulator.gestures.any { it.second.let { g -> g is EmulatorGesture.Touch && g.action == TouchAction.UP } }
        }
        assertTrue(emulator.gestures.all { it.first == RunningAndroidDevice.id })
        val touches = emulator.gestures.map { it.second }.filterIsInstance<EmulatorGesture.Touch>()
        assertEquals(
            listOf(
                EmulatorGesture.Touch(TouchAction.DOWN, TestFrameWidth / 2, TestFrameHeight / 2, TestFrameWidth, TestFrameHeight),
                EmulatorGesture.Touch(TouchAction.UP, TestFrameWidth / 2, TestFrameHeight / 2, TestFrameWidth, TestFrameHeight),
            ),
            touches,
        )
    }

    @Test
    fun dragOnTheDeviceTabIsSentAsSwipe() = runComposeUiTest {
        val emulator = streaming(RunningAndroidDevice)
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(emulator, workspace = workspace)
        val tabId = openDeviceTab(workspace, RunningAndroidDevice.id)
        awaitFrame(tabId)

        onNode(hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId))))
            .performTouchInput { swipe(start = centerLeft, end = centerRight) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            emulator.gestures.lastOrNull()?.second.let { it is EmulatorGesture.Touch && it.action == TouchAction.UP }
        }
        assertTrue(emulator.gestures.all { it.first == RunningAndroidDevice.id })
        val touches = emulator.gestures.map { it.second }.filterIsInstance<EmulatorGesture.Touch>()
        assertEquals(TouchAction.DOWN, touches.first().action)
        assertEquals(0, touches.first().x)
        assertEquals(TestFrameHeight / 2, touches.first().y)
        assertEquals(TouchAction.UP, touches.last().action)
        assertEquals(TestFrameWidth - 1, touches.last().x)
        assertTrue(touches.any { it.action == TouchAction.MOVE })
    }

    @Test
    fun uncontrollableDeviceTabSaysSoAndSendsNothing() = runComposeUiTest {
        val emulator = streaming(RunningSimulator)
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(emulator, workspace = workspace)
        val tabId = openDeviceTab(workspace, RunningSimulator.id)
        awaitFrame(tabId)

        onNodeWithText("이 기기에는 제스처를 보낼 수 없습니다. 화면만 볼 수 있습니다.").assertIsDisplayed()
        onNode(hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId))))
            .performTouchInput { click(center) }
        waitForIdle()
        assertTrue(emulator.gestures.isEmpty())
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
