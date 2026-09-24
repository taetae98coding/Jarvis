package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.automation.AgentBrowserTab
import io.github.taetae98coding.jarvis.automation.AgentTabs
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import kotlinx.coroutines.flow.first

/**
 * Claude 가 쓴 브라우저·기기를 그 Claude 탭의 패널에 붙인다(docs/common/mcp-server.html R3–R8). 찾기와 붙이기를
 * [TerminalWorkspaceRepository.updateWorkspace] 한 번 안에서 해서, 도구 두 개가 동시에 불려도 같은 기기 탭이 두 번
 * 생기지 않는다.
 */
internal class WorkspaceAgentTabs(
    private val repository: TerminalWorkspaceRepository,
) : AgentTabs {
    override suspend fun hasCaller(sessionId: String): Boolean =
        repository.observeWorkspace().first().findClaudeTab(sessionId) != null

    override suspend fun openBrowserTab(sessionId: String, url: String): Long? {
        var opened: Long? = null
        repository.updateWorkspace { workspace ->
            val caller = workspace.findClaudeTab(sessionId) ?: return@updateWorkspace workspace
            opened = workspace.nextId
            workspace.appendTab(caller.group.id, TerminalProgram.Browser, url = url)
        }

        return opened
    }

    override suspend fun browserTabs(sessionId: String): List<AgentBrowserTab> {
        val caller = repository.observeWorkspace().first().findClaudeTab(sessionId) ?: return emptyList()

        return caller.panel.tabs
            .filter { it.program == TerminalProgram.Browser }
            .map { AgentBrowserTab(it.id, it.url ?: TerminalTab.DefaultBrowserUrl) }
    }

    override suspend fun showDevice(sessionId: String, deviceId: String, deviceName: String) {
        repository.updateWorkspace { workspace ->
            val caller = workspace.findClaudeTab(sessionId) ?: return@updateWorkspace workspace
            val shown = caller.panel.tabs.any { it.program == TerminalProgram.Device && it.deviceId == deviceId }

            if (shown) {
                workspace
            } else {
                workspace.appendTab(caller.group.id, TerminalProgram.Device, deviceId = deviceId, deviceName = deviceName)
            }
        }
    }

    // 브라우저 탭만 닫는다. Claude 가 셸·Claude 탭을 닫을 도구는 없다.
    override suspend fun closeTab(sessionId: String, tabId: Long): Boolean {
        var closed = false
        repository.updateWorkspace { workspace ->
            val caller = workspace.findClaudeTab(sessionId) ?: return@updateWorkspace workspace
            val tab = caller.panel.tabs.firstOrNull { it.id == tabId && it.program == TerminalProgram.Browser }
                ?: return@updateWorkspace workspace
            closed = true
            workspace.closeTab(tab.id)
        }

        return closed
    }
}
