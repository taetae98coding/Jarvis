package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.automation.AgentBrowserTab
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceChange
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceAgentTabsTest {
    // 패널 1: 그룹 둘(왼쪽 셸, 오른쪽 Claude). 패널 2 가 선택돼 있다.
    private val initial: TerminalWorkspace = TerminalWorkspace.initial()
        .split(SplitDirection.SideBySide)
        .let { it.addTab(it.focusedGroup!!.id, TerminalProgram.Claude, claudeSessionId = Session) }
        .addPanel()
        .addTab()

    private val repository = FakeWorkspaceRepository(initial)
    private val tabs = WorkspaceAgentTabs(repository)

    private val claudeGroupId: Long = initial.findClaudeTab(Session)!!.group.id

    @Test
    fun browserTabGoesToTheCallerGroupUnselected() = runTest {
        val tabId = tabs.openBrowserTab(Session, "https://example.com")!!

        val after = repository.state.value
        val group = after.findClaudeTab(Session)!!.group
        assertEquals(claudeGroupId, group.id)
        assertEquals(tabId, group.tabs.last().id)
        assertEquals(TerminalProgram.Browser, group.tabs.last().program)
        assertEquals(initial.findClaudeTab(Session)!!.group.selectedTabId, group.selectedTabId)
        assertEquals(initial.selectedPanelId, after.selectedPanelId)
        assertEquals(listOf(AgentBrowserTab(tabId, "https://example.com")), tabs.browserTabs(Session))
    }

    @Test
    fun unknownCallerGetsNoTab() = runTest {
        assertFalse(tabs.hasCaller("other"))
        assertNull(tabs.openBrowserTab("other", "https://example.com"))
        tabs.showDevice("other", "emulator-5554", "Pixel")
        assertEquals(initial, repository.state.value)
        assertEquals(emptyList(), tabs.browserTabs("other"))
    }

    @Test
    fun deviceTabIsAddedOncePerPanel() = runTest {
        assertTrue(tabs.hasCaller(Session))

        tabs.showDevice(Session, "emulator-5554", "Pixel")
        tabs.showDevice(Session, "emulator-5554", "Pixel")

        val panel = repository.state.value.findClaudeTab(Session)!!.panel
        val devices = panel.tabs.filter { it.program == TerminalProgram.Device }
        assertEquals(listOf("emulator-5554"), devices.map { it.deviceId })
        assertEquals("Pixel", devices.single().deviceName)
    }

    @Test
    fun closesOnlyBrowserTabsOfTheCallerPanel() = runTest {
        val tabId = tabs.openBrowserTab(Session, "https://example.com")!!
        val shellInOtherPanel = initial.panels.last().tabs.single().id
        val claudeTab = initial.findClaudeTab(Session)!!.tab.id

        assertFalse(tabs.closeTab(Session, shellInOtherPanel))
        assertFalse(tabs.closeTab(Session, claudeTab))
        assertTrue(tabs.closeTab(Session, tabId))
        assertEquals(emptyList(), tabs.browserTabs(Session))
    }

    private class FakeWorkspaceRepository(initial: TerminalWorkspace) : TerminalWorkspaceRepository {
        val state = MutableStateFlow(initial)

        override fun observeWorkspace(): Flow<TerminalWorkspace> = state

        override suspend fun updateWorkspace(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange {
            val before = state.value
            state.value = transform(before)
            return TerminalWorkspaceChange(before, state.value)
        }
    }

    private companion object {
        const val Session = "session-1"
    }
}
