package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.ObserveDirectoryUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitGraphUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitStatusUseCase
import io.github.taetae98coding.jarvis.domain.terminal.StageGitChangesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.UnstageGitChangesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed interface FileTreeState {
    data object Loading : FileTreeState

    data object NoFolder : FileTreeState

    data class Unreadable(val root: String) : FileTreeState

    data class Loaded(val root: String, val rows: List<FileTreeRow>) : FileTreeState
}

/** 트리를 펼친 순서대로 편 한 줄. [depth] 는 기준 폴더 바로 아래가 0 이다. */
internal data class FileTreeRow(
    val entry: FileEntry,
    val depth: Int,
    val expanded: Boolean,
)

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

    /** 마지막 stage·unstage 가 실패했으면 git 의 오류 문구. */
    val gitError: StateFlow<String?> = mutableGitError.asStateFlow()

    fun setDirectory(value: String?) {
        directory.value = value
    }

    fun toggle(path: String) = expanded.update { if (path in it) it - path else it + path }

    fun stage(root: String, changes: List<GitChange>) = runGit { stageGitChanges(root, changes) }

    fun unstage(root: String, changes: List<GitChange>) = runGit { unstageGitChanges(root, changes) }

    private fun runGit(command: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            mutableGitError.value = command().exceptionOrNull()?.let { it.message ?: "git 이 실패했습니다" }
        }
    }

    // 펼친 폴더마다 그 폴더를 따로 관측해서, 안쪽 폴더가 바뀌어도 그 가지만 다시 읽는다.
    private fun rows(entries: List<FileEntry>, depth: Int, open: Set<String>): Flow<List<FileTreeRow>> {
        if (entries.isEmpty()) return flowOf(emptyList())

        val branches = entries.map { entry ->
            val row = FileTreeRow(entry, depth, expanded = entry.isDirectory && entry.path in open)
            if (!row.expanded) {
                flowOf(listOf(row))
            } else {
                observeDirectory(entry.path)
                    .flatMapLatest { children -> rows(children.orEmpty(), depth + 1, open) }
                    .map { listOf(row) + it }
            }
        }

        return combine(branches) { parts -> parts.flatMap { it } }
    }
}
