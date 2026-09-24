package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

/** 패널·탭·창 배치의 저장소. 앱을 다시 켜도 같은 값이 온다. */
interface TerminalWorkspaceRepository {
    fun observeWorkspace(): Flow<TerminalWorkspace>

    /** 여러 곳에서 동시에 불려도 앞의 변경 위에 다음 변경이 쌓인다. */
    suspend fun updateWorkspace(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange
}

data class TerminalWorkspaceChange(
    val before: TerminalWorkspace,
    val after: TerminalWorkspace,
) {
    /** 이번 변경으로 작업 공간에서 사라진 창. */
    val removedLeaves: List<PaneNode.Leaf>
        get() {
            val remaining = after.paneIds.toSet()

            return before.leaves.filter { it.paneId !in remaining }
        }
}
