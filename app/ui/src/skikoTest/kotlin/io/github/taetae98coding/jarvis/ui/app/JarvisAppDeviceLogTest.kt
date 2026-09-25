package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.emulator.DeviceLogClearTestTag
import io.github.taetae98coding.jarvis.ui.emulator.DeviceLogFilterTestTag
import io.github.taetae98coding.jarvis.ui.emulator.DeviceLogLineTestTag
import io.github.taetae98coding.jarvis.ui.emulator.DeviceLogTestTag
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorFrameTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalDeviceLogToggleTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalDeviceTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** docs/common/device-logcat.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppDeviceLogTest {
    private val emulator = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice), frames = MutableStateFlow(TestFrame))

    private val deviceLog = FakeDeviceLogRepository()

    /** 셸 탭 하나와 그 뒤에 선택된 [RunningAndroidDevice] 기기 탭이 있는 작업 공간으로 터미널을 연다. 기기 탭의 id 를 돌려준다. */
    private fun ComposeUiTest.openDeviceTab(logVisible: Boolean = false): Pair<FakeTerminalWorkspaceRepository, Long> {
        val initial = TerminalWorkspace.initial()
            .addTab(program = TerminalProgram.Device, deviceId = RunningAndroidDevice.id, deviceName = RunningAndroidDevice.name, devicePlatform = DevicePlatform.Android)
            .let { it.setDeviceLogVisible(it.focusedTab!!.id, logVisible) }
        val workspace = FakeTerminalWorkspaceRepository(initial)
        val tabId = initial.focusedTab!!.id

        setContent { TestJarvisApp(emulator = emulator, deviceLog = deviceLog, terminalWorkspace = workspace) }
        onNodeWithTag(TerminalTestTag).performClick()
        val frame = hasTestTag(EmulatorFrameTestTag) and hasAnyAncestor(hasTestTag(terminalDeviceTestTag(tabId)))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodes(frame).fetchSemanticsNodes().isNotEmpty() }

        return workspace to tabId
    }

    private fun ComposeUiTest.awaitLogReader(present: Boolean) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { (deviceLog.of(RunningAndroidDevice.id).subscriptionCount.value > 0) == present }
    }

    // R1, R9
    @Test
    fun toggleShowsAndHidesTheLog() = runComposeUiTest {
        val (workspace, tabId) = openDeviceTab()
        val toggle = onNodeWithTag(terminalDeviceLogToggleTestTag(tabId))

        toggle.assertContentDescriptionEquals("로그 보기")
        assertTrue(onAllNodesWithTag(DeviceLogTestTag).fetchSemanticsNodes().isEmpty())
        assertEquals(0, deviceLog.of(RunningAndroidDevice.id).subscriptionCount.value)

        toggle.performClick()

        onNodeWithTag(DeviceLogTestTag).assertIsDisplayed()
        onNodeWithText("로그를 기다리는 중…").assertIsDisplayed()
        toggle.assertContentDescriptionEquals("로그 숨기기")
        assertTrue(workspace.workspace.value.tabs.single { it.id == tabId }.deviceLogVisible)

        awaitLogReader(present = true)
        deviceLog.of(RunningAndroidDevice.id).tryEmit(listOf("09-25 17:18:51.832  1850  2652 E Tag: boom"))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(DeviceLogLineTestTag).fetchSemanticsNodes().isNotEmpty() }
        onNode(hasText("09-25 17:18:51.832  1850  2652 E Tag: boom") and hasTestTag(DeviceLogLineTestTag)).assertIsDisplayed()

        toggle.performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(DeviceLogTestTag).fetchSemanticsNodes().isEmpty() }
        assertFalse(workspace.workspace.value.tabs.single { it.id == tabId }.deviceLogVisible)
        awaitLogReader(present = false)
    }

    // R2: 다른 탭을 골랐다 돌아와도 로그 창이 남는다. 가려진 동안에는 읽지 않는다(R9).
    @Test
    fun visibilityIsKeptWhenTheTabIsHiddenAndShownAgain() = runComposeUiTest {
        val (workspace, tabId) = openDeviceTab(logVisible = true)
        onNodeWithTag(DeviceLogTestTag).assertIsDisplayed()
        awaitLogReader(present = true)

        val shellTab = workspace.workspace.value.tabs.first { it.program == TerminalProgram.Shell }
        onNodeWithTag(terminalTabTestTag(shellTab.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(DeviceLogTestTag).fetchSemanticsNodes().isEmpty() }
        awaitLogReader(present = false)

        onNodeWithTag(terminalTabTestTag(tabId)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(DeviceLogTestTag).fetchSemanticsNodes().isNotEmpty() }
        awaitLogReader(present = true)
    }

    // R7
    @Test
    fun filterAndClearNarrowTheLines() = runComposeUiTest {
        openDeviceTab(logVisible = true)
        awaitLogReader(present = true)
        deviceLog.of(RunningAndroidDevice.id).tryEmit(listOf("ActivityManager: start", "Choreographer: skipped"))
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(DeviceLogLineTestTag).fetchSemanticsNodes().size == 2 }

        onNodeWithTag(DeviceLogFilterTestTag).performTextInput("choreo")
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(DeviceLogLineTestTag).fetchSemanticsNodes().size == 1 }
        onNode(hasText("Choreographer: skipped") and hasTestTag(DeviceLogLineTestTag)).assertIsDisplayed()

        onNodeWithTag(DeviceLogFilterTestTag).performTextInput("zzz")
        onNodeWithText("필터에 맞는 줄이 없습니다").assertIsDisplayed()

        onNodeWithTag(DeviceLogClearTestTag).performClick()
        onNodeWithText("로그를 기다리는 중…").assertIsDisplayed()
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}
