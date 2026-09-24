package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.DockEdge
import io.github.taetae98coding.jarvis.domain.terminal.IsClaudeSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveTerminalWorkspaceUseCase
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.domain.terminal.UpdateTerminalWorkspaceUseCase
import io.github.taetae98coding.jarvis.domain.terminal.newClaudeSessionId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 저장된 작업 공간(패널·그룹·탭)을 보여 주고 바꾼다. 탭의 셸은 [TerminalPaneHost] 가 들고 있어서, 이
 * ViewModel 이 치워져도 셸은 이어진다. 분할·닫기 규칙은 domain 에만 있다.
 */
internal class TerminalViewModel(
    observeWorkspace: ObserveTerminalWorkspaceUseCase,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
    private val host: TerminalPaneHost,
    isClaudeSupported: IsClaudeSupportedUseCase,
) : ViewModel() {
    val isClaudeSupported: Boolean = isClaudeSupported()

    /** null 은 저장된 배치를 아직 읽지 못한 것이다. */
    val workspace: StateFlow<TerminalWorkspace?> = observeWorkspace()
        .onEach(::reconcile)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun pane(tabId: Long): TerminalPaneState? = host.pane(tabId)

    fun addPanel() = update { it.addPanel() }

    fun renamePanel(panelId: Long, name: String) = update { it.renamePanel(panelId, name) }

    fun closePanel(panelId: Long) = update { it.closePanel(panelId) }

    fun selectPanel(panelId: Long) = update { it.selectPanel(panelId) }

    fun splitSideBySide() = update { it.split(SplitDirection.SideBySide, it.focusedTab?.directory) }

    fun splitStacked() = update { it.split(SplitDirection.Stacked, it.focusedTab?.directory) }

    /** [groupId] 가 null 이면 포커스된 그룹(없으면 새 그룹)이다. 새 탭은 그 그룹에서 선택돼 있던 탭의 디렉터리에서 시작한다. */
    fun addTab(groupId: Long? = null) = update { it.addTab(groupId, directory = it.directoryOf(groupId)) }

    fun addClaudeTab(groupId: Long? = null) {
        val sessionId = newClaudeSessionId()
        update { it.addTab(groupId, TerminalProgram.Claude, it.directoryOf(groupId), sessionId) }
    }

    fun closeFocusedTab() = update { it.closeFocusedTab() }

    fun closeTab(tabId: Long) = update { it.closeTab(tabId) }

    fun selectTab(tabId: Long) = update { it.selectTab(tabId) }

    fun selectTabAt(index: Int) = update { it.selectTabAt(index) }

    fun selectAdjacentTab(offset: Int) = update { it.selectAdjacentTab(offset) }

    fun focusGroup(groupId: Long) = update { it.focusGroup(groupId) }

    fun focusAdjacentGroup(offset: Int) = update { it.focusAdjacentGroup(offset) }

    fun dockTab(tabId: Long, groupId: Long, edge: DockEdge) = update { it.dockTab(tabId, groupId, edge) }

    fun setRatio(splitId: Long, ratio: Float) = update { it.setRatio(splitId, ratio) }

    private fun TerminalWorkspace.directoryOf(groupId: Long?): String? {
        val group: PaneNode.Group? = if (groupId == null) focusedGroup else groups.firstOrNull { it.id == groupId }

        return group?.selectedTab?.directory
    }

    private fun update(transform: (TerminalWorkspace) -> TerminalWorkspace) {
        viewModelScope.launch {
            val change = updateWorkspace(transform)
            host.release(change.removedTabs.map { it.id })
        }
    }

    /**
     * 사라진 탭의 셸을 닫고, 지금 보이는 탭(선택된 패널에서 그룹마다 선택된 탭)의 창을 연다. 다른 탭·패널의
     * 창은 처음 보일 때 연다 — 앱을 켜자마자 모든 창을 띄우면 Claude 탭마다 백그라운드 세션을 찾는 셸이
     * 한꺼번에 돈다.
     */
    private fun reconcile(workspace: TerminalWorkspace) {
        host.retain(workspace.tabIds.toSet())

        val visible = workspace.visibleTabs

        // 새 창은 아직 배치되지 않았다. 이미 배치된 창의 크기로 먼저 띄우면 배치된 뒤의 크기와 가까워서,
        // 셸이 첫 프롬프트를 엉뚱한 너비로 그렸다가 다시 그리는 일이 줄어든다.
        val size = visible.firstNotNullOfOrNull { host.pane(it.id)?.emulator }
            ?.let { TerminalSize(it.columns, it.rows) }
            ?: TerminalSize.Default

        visible.forEach { host.acquire(it, size) }
    }
}
