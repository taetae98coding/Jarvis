package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
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
        openTerminal(streaming(RunningAndroidDevice, StoppedAndroidDevice, PhysicalAndroidDevice, RunningSimulator, PhysicalIosDevice))

        onNode(newTabButton).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) {
            onAllNodesWithTag(terminalNewDeviceTabTestTag(RunningAndroidDevice.id)).fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithText("기기").assertIsDisplayed()
        onNodeWithTag(terminalNewDeviceTabTestTag(RunningAndroidDevice.id)).assertIsDisplayed()
        onNodeWithText("Android 에뮬레이터").assertIsDisplayed()
        onNodeWithTag(terminalNewDeviceTabTestTag(PhysicalAndroidDevice.id)).assertIsDisplayed()
        onNodeWithTag(terminalNewDeviceTabTestTag(RunningSimulator.id)).assertIsDisplayed()
        onNodeWithText("Android 실물 기기").assertIsDisplayed()
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

    @Test
    fun tapOnTheDeviceTabIsSentInDeviceCoordinates() = runComposeUiTest {
        val emulator = streaming(RunningAndroidDevice)
        val workspace = FakeTerminalWorkspaceRepository()
        openTerminal(emulator, workspace = workspace)
        val tabId = openDeviceTab(workspace, RunningAndroidDevice.id)
        awaitFrame(tabId)

        onNode(hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId))))
            .performTouchInput { click(center) }

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.gestures.isNotEmpty() }
        assertEquals(
            listOf(RunningAndroidDevice.id to EmulatorGesture.Tap(x = TestFrameWidth / 2, y = TestFrameHeight / 2)),
            emulator.gestures.toList(),
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

        waitUntil(timeoutMillis = FrameTimeoutMillis) { emulator.gestures.isNotEmpty() }
        val (deviceId, gesture) = emulator.gestures.single()
        val swipe = assertIs<EmulatorGesture.Swipe>(gesture)
        assertEquals(RunningAndroidDevice.id, deviceId)
        assertEquals(0, swipe.fromX)
        assertEquals(TestFrameWidth - 1, swipe.toX)
        assertEquals(TestFrameHeight / 2, swipe.fromY)
        assertTrue(swipe.durationMillis >= 50)
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
