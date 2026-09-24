package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.OpenTerminalSessionUseCase
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 작업 공간(탭·분할)과 패널마다의 셸. 작업 공간이 바뀔 때마다 패널 id 목록에 셸을 맞춘다 — 새 id 에는
 * 셸을 띄우고 사라진 id 의 셸은 닫는다. 그래서 분할·닫기 규칙은 domain 에만 있다.
 */
internal class TerminalViewModel(
    private val openSession: OpenTerminalSessionUseCase,
) : ViewModel() {
    private val _workspace = MutableStateFlow(TerminalWorkspace.initial())
    val workspace: StateFlow<TerminalWorkspace> = _workspace.asStateFlow()

    private val panes = mutableMapOf<Long, TerminalPaneState>()

    init {
        reconcile(sizeSource = null)
    }

    fun pane(id: Long): TerminalPaneState? = panes[id]

    fun splitSideBySide() = update { it.split(SplitDirection.SideBySide) }

    fun splitStacked() = update { it.split(SplitDirection.Stacked) }

    fun addTab() = update { it.addTab() }

    fun closeFocusedPane() = update { workspace -> workspace.focusedPaneId?.let(workspace::closePane) ?: workspace }

    fun closePane(paneId: Long) = update { it.closePane(paneId) }

    fun closeTab(tabId: Long) = update { it.closeTab(tabId) }

    fun selectTab(tabId: Long) = update { it.selectTab(tabId) }

    fun selectTabAt(index: Int) = update { it.selectTabAt(index) }

    fun selectAdjacentTab(offset: Int) = update { it.selectAdjacentTab(offset) }

    fun focusPane(paneId: Long) = update { it.focusPane(paneId) }

    fun focusAdjacentPane(offset: Int) = update { it.focusAdjacentPane(offset) }

    fun resizeSplit(splitId: Long, delta: Float) = update { it.resizeSplit(splitId, delta) }

    override fun onCleared() {
        panes.values.forEach(TerminalPaneState::close)
        panes.clear()
    }

    private fun update(transform: (TerminalWorkspace) -> TerminalWorkspace) {
        val previous = _workspace.value
        val next = transform(previous)
        if (next == previous) return

        _workspace.value = next
        reconcile(sizeSource = previous.focusedPaneId?.let(panes::get))
    }

    private fun reconcile(sizeSource: TerminalPaneState?) {
        val ids = _workspace.value.paneIds.toSet()

        // 새 패널은 아직 배치되지 않았다. 직전에 포커스된 패널의 크기로 먼저 띄우면 배치된 뒤의 크기와
        // 가까워서, 셸이 첫 프롬프트를 엉뚱한 너비로 그렸다가 다시 그리는 일이 줄어든다.
        val size = sizeSource?.emulator?.let { TerminalSize(it.columns, it.rows) } ?: TerminalSize.Default

        (panes.keys - ids).forEach { id -> panes.remove(id)?.close() }

        (ids - panes.keys).forEach { id ->
            panes[id] = TerminalPaneState(
                id = id,
                initialSize = size,
                scope = viewModelScope,
                open = { openSession(it) },
                onExit = ::closePane,
            )
        }
    }
}
