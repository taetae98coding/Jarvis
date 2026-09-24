package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.OpenTerminalSessionUseCase
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.UpdateTerminalWorkspaceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 창 id 마다 세션과 에뮬레이터를 든다. 터미널 화면을 벗어나 ViewModel 이 치워져도 닫지 않는다 — 돌아오면
 * 같은 셸과 같은 화면이 보여야 한다. 사용자가 창을 닫거나 셸이 끝나야 닫는다.
 *
 * 앱 수명 스코프를 쓰는 것은 상태 구독이 아니라 사용자가 띄운 프로세스를 붙잡는 일이라서다
 * (docs/common/terminal-panels.html#implementation). 에뮬레이터는 메인에서만 만지므로 스코프도 메인이다.
 */
internal class TerminalPaneHost(
    private val openSession: OpenTerminalSessionUseCase,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val panes = mutableMapOf<Long, TerminalPaneState>()

    fun pane(paneId: Long): TerminalPaneState? = panes[paneId]

    /** 처음 불릴 때 세션을 연다. 이미 열려 있으면 그것을 돌려준다. */
    fun acquire(leaf: PaneNode.Leaf, initialSize: TerminalSize): TerminalPaneState =
        panes.getOrPut(leaf.paneId) {
            TerminalPaneState(
                id = leaf.paneId,
                initialSize = initialSize,
                scope = scope,
                open = { size -> openSession(size, leaf) },
                onExit = ::onExit,
                onDirectory = ::onDirectory,
            )
        }

    fun release(paneIds: Collection<Long>) {
        paneIds.forEach { panes.remove(it)?.close() }
    }

    /** 작업 공간에 없는 창의 세션을 닫는다. */
    fun retain(paneIds: Set<Long>) {
        release(panes.keys - paneIds)
    }

    fun close() {
        release(panes.keys.toList())
        scope.cancel()
    }

    // 화면이 보이지 않아도 창이 닫혀야 해서 ViewModel 을 거치지 않고 직접 바꾼다.
    private fun onExit(paneId: Long) {
        release(listOf(paneId))
        scope.launch {
            val change = updateWorkspace { it.closePane(paneId) }
            release(change.removedLeaves.map { it.paneId })
        }
    }

    private fun onDirectory(paneId: Long, directory: String) {
        scope.launch { updateWorkspace { it.setDirectory(paneId, directory) } }
    }
}
