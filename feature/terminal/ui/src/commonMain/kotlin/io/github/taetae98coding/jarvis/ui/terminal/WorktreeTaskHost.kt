package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AddWorktreePanelUseCase
import io.github.taetae98coding.jarvis.domain.terminal.CloseWorktreePanelUseCase
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 만드는 중인 워크트리. [parent] 는 "새 워크트리" 창을 열 때 관측한 main 패널의 워크트리다. */
internal data class PendingWorktree(
    val id: Long,
    val parentId: Long,
    val parent: GitWorktree,
    val branch: String,
    val baseBranch: String?,
    val directory: String,
)

/** 뒤에서 돌다 실패해서 창을 다시 띄워야 하는 것. 창은 누를 때의 값으로 시작한다. */
internal sealed interface WorktreeFailure {
    val id: Long
    val message: String

    data class Create(val pending: PendingWorktree, override val message: String) : WorktreeFailure {
        override val id: Long get() = pending.id
    }

    data class Remove(
        override val id: Long,
        val panelId: Long,
        val worktree: GitWorktree,
        val removeWorktree: Boolean,
        val deleteDirectory: Boolean,
        override val message: String,
    ) : WorktreeFailure
}

/**
 * 워크트리 만들기·지우기를 창과 화면의 수명과 무관하게 끝까지 돌리고, 도는 것과 실패한 것을 든다.
 * ViewModel 에 두면 터미널 화면을 벗어날 때 git 의 결과를 잃어 워크트리만 생기거나 지워진 워크트리의 패널이
 * 남는다. 관측이 아니라 사용자가 띄운 명령의 진행이라 [TerminalPaneHost] 처럼 앱 수명 스코프를 쓴다
 * (docs/common/terminal-worktree.html#implementation).
 */
internal class WorktreeTaskHost(
    private val addWorktree: AddWorktreePanelUseCase,
    private val closeWorktree: CloseWorktreePanelUseCase,
    private val paneHost: TerminalPaneHost,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var nextId = 0L

    private val _pending = MutableStateFlow<List<PendingWorktree>>(emptyList())
    val pending: StateFlow<List<PendingWorktree>> = _pending.asStateFlow()

    private val _removing = MutableStateFlow<Set<Long>>(emptySet())
    val removing: StateFlow<Set<Long>> = _removing.asStateFlow()

    private val _failures = MutableStateFlow<List<WorktreeFailure>>(emptyList())
    val failures: StateFlow<List<WorktreeFailure>> = _failures.asStateFlow()

    fun add(parentId: Long, parent: GitWorktree, branch: String, baseBranch: String?, directory: String, claudeSessionId: String?) {
        val task = PendingWorktree(nextId++, parentId, parent, branch, baseBranch, directory)
        _pending.update { it + task }

        scope.launch {
            val result = addWorktree(parentId, branch, baseBranch, directory, claudeSessionId)
            _pending.update { tasks -> tasks.filterNot { it.id == task.id } }
            result.onFailure { error -> fail(WorktreeFailure.Create(task, error.message(default = "워크트리를 만들지 못했습니다"))) }
        }
    }

    fun remove(panelId: Long, worktree: GitWorktree, removeWorktree: Boolean, deleteDirectory: Boolean) {
        if (panelId in _removing.value) return
        _removing.update { it + panelId }

        scope.launch {
            val result = closeWorktree(panelId, removeWorktree, deleteDirectory)
            result.onSuccess { change -> paneHost.release(change.removedTabs.map { it.id }) }
            _removing.update { it - panelId }
            result.onFailure { error ->
                fail(
                    WorktreeFailure.Remove(
                        id = nextId++,
                        panelId = panelId,
                        worktree = worktree,
                        removeWorktree = removeWorktree,
                        deleteDirectory = deleteDirectory,
                        message = error.message(default = "워크트리를 지우지 못했습니다"),
                    ),
                )
            }
        }
    }

    fun dismissFailure(id: Long) {
        _failures.update { failures -> failures.filterNot { it.id == id } }
    }

    fun close() {
        scope.cancel()
    }

    private fun fail(failure: WorktreeFailure) {
        _failures.update { it + failure }
    }

    private fun Throwable.message(default: String): String = message?.takeIf(String::isNotBlank) ?: default
}
