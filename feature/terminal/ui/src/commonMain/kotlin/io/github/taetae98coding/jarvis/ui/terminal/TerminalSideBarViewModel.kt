package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.ObserveDirectoryUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitGraphUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitStatusUseCase
import io.github.taetae98coding.jarvis.domain.terminal.PushGitBranchUseCase
import io.github.taetae98coding.jarvis.domain.terminal.StageGitChangesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.UnstageGitChangesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 폴더 하나뿐인 폴더를 이어 펼치고 한 줄로 합치는 최대 단계 (docs/common/terminal-side-bar.html R8a, R8b). */
internal const val FileTreeChainMaxDepth = 32

internal sealed interface FileTreeState {
    data object Loading : FileTreeState

    data object NoFolder : FileTreeState

    data class Unreadable(val root: String) : FileTreeState

    data class Loaded(val root: String, val rows: List<FileTreeRow>) : FileTreeState
}

/**
 * 트리를 펼친 순서대로 편 한 줄. [depth] 는 기준 폴더 바로 아래가 0 이다. [chain] 은 폴더 하나뿐인 폴더를 합친
 * 첫 항목 … 끝 항목이고(R8b), 합치지 않았으면 하나다. [expanded] 는 끝 항목이 펼쳐졌는지다.
 */
internal data class FileTreeRow(
    val chain: List<FileEntry>,
    val depth: Int,
    val expanded: Boolean,
) {
    val first: FileEntry get() = chain.first()

    val last: FileEntry get() = chain.last()

    val name: String get() = chain.joinToString("/") { it.name }

    // 펼친 합친 줄은 첫 폴더를 접어 한 폴더로 돌아가고, 접힌 줄은 끝 폴더를 펼친다.
    val toggleTarget: String get() = if (expanded) first.path else last.path
}

internal sealed interface GitPanelState {
    data object Loading : GitPanelState

    data object NoFolder : GitPanelState

    data object NotRepository : GitPanelState

    data class Loaded(val status: GitStatus) : GitPanelState
}

/**
 * 오른쪽 사이드 바의 파일 트리와 Git 구획. 기준 폴더는 화면이 [setDirectory] 로 넘긴다. 모든 조회는 구획이 보이는
 * 동안만 돈다 — 가려지면 구독이 끝나고, 다시 보이면 이전 값 대신 처음부터 읽는다(docs/common/terminal-side-bar.html R6).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TerminalSideBarViewModel(
    private val observeDirectory: ObserveDirectoryUseCase,
    observeGitStatus: ObserveGitStatusUseCase,
    observeGitGraph: ObserveGitGraphUseCase,
    private val stageGitChanges: StageGitChangesUseCase,
    private val unstageGitChanges: UnstageGitChangesUseCase,
    private val pushGitBranch: PushGitBranchUseCase,
) : ViewModel() {
    private val directory = MutableStateFlow<String?>(null)

    // 절대 경로다. 기준 폴더를 바꿔도 지우지 않아서 돌아오면 펼친 채다.
    private val expanded = MutableStateFlow<Set<String>>(emptySet())

    val fileTree: StateFlow<FileTreeState> = directory
        .flatMapLatest { root ->
            if (root == null) return@flatMapLatest flowOf(FileTreeState.NoFolder)

            observeDirectory(root).flatMapLatest { entries ->
                if (entries == null) {
                    flowOf(FileTreeState.Unreadable(root))
                } else {
                    expanded.flatMapLatest { open -> rows(entries, depth = 0, open = open) }.map { FileTreeState.Loaded(root, it) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), FileTreeState.Loading)

    val gitStatus: StateFlow<GitPanelState> = directory
        .flatMapLatest { root ->
            if (root == null) {
                flowOf(GitPanelState.NoFolder)
            } else {
                observeGitStatus(root).map { status -> status?.let { GitPanelState.Loaded(it) } ?: GitPanelState.NotRepository }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), GitPanelState.Loading)

    /** null 은 아직 읽지 못한 것이다. */
    val gitGraph: StateFlow<List<GitGraphLine>?> = directory
        .flatMapLatest { root -> if (root == null) flowOf(emptyList()) else observeGitGraph(root) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    private val mutableGitError = MutableStateFlow<String?>(null)

    /** 마지막 stage·unstage·push 가 실패했으면 git 의 오류 문구. */
    val gitError: StateFlow<String?> = mutableGitError.asStateFlow()

    private val mutablePushing = MutableStateFlow(false)

    val pushing: StateFlow<Boolean> = mutablePushing.asStateFlow()

    fun setDirectory(value: String?) {
        directory.value = value
    }

    fun toggle(path: String) {
        if (path in expanded.value) {
            expanded.update { it - path }
            return
        }

        expanded.update { it + path }
        viewModelScope.launch { expanded.update { it + singleFolderChain(path) } }
    }

    // 폴더 심볼릭 링크를 따라가므로 `a/link → a` 처럼 끝없이 이어질 수 있어서 단계 상한을 둔다.
    private suspend fun singleFolderChain(path: String): List<String> {
        val chain = mutableListOf<String>()
        var current = path
        repeat(FileTreeChainMaxDepth) {
            val only = observeDirectory(current).first()?.singleOrNull()?.takeIf { it.isDirectory } ?: return chain
            chain += only.path
            current = only.path
        }
        return chain
    }

    fun stage(root: String, changes: List<GitChange>) = runGit { stageGitChanges(root, changes) }

    fun unstage(root: String, changes: List<GitChange>) = runGit { unstageGitChanges(root, changes) }

    // 도는 동안 다시 누르면 같은 커밋을 두 번 올리려는 push 가 겹친다(docs/common/terminal-side-bar.html R23).
    fun push(root: String, target: GitPushTarget) {
        if (!mutablePushing.compareAndSet(expect = false, update = true)) return

        runGit {
            try {
                pushGitBranch(root, target)
            } finally {
                mutablePushing.value = false
            }
        }
    }

    private fun runGit(command: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            mutableGitError.value = command().exceptionOrNull()?.let { it.message ?: "git 이 실패했습니다" }
        }
    }

    // 펼친 폴더마다 그 폴더를 따로 관측해서, 안쪽 폴더가 바뀌어도 그 가지만 다시 읽는다.
    private fun rows(entries: List<FileEntry>, depth: Int, open: Set<String>): Flow<List<FileTreeRow>> {
        if (entries.isEmpty()) return flowOf(emptyList())

        return combine(entries.map { branch(listOf(it), depth, open) }) { parts -> parts.flatMap { it } }
    }

    // 접힌 폴더는 안을 읽지 않으므로 합치지 않는다(R8b).
    private fun branch(chain: List<FileEntry>, depth: Int, open: Set<String>): Flow<List<FileTreeRow>> {
        val last = chain.last()
        if (!last.isDirectory || last.path !in open) return flowOf(listOf(FileTreeRow(chain, depth, expanded = false)))

        return observeDirectory(last.path).flatMapLatest { children ->
            val only = children?.singleOrNull()?.takeIf { it.isDirectory }
            if (only != null && chain.size < FileTreeChainMaxDepth) {
                branch(chain + only, depth, open)
            } else {
                rows(children.orEmpty(), depth + 1, open).map { listOf(FileTreeRow(chain, depth, expanded = true)) + it }
            }
        }
    }
}
