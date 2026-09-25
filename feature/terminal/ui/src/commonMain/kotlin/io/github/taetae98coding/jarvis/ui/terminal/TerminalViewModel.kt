package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeTabStatus
import io.github.taetae98coding.jarvis.domain.terminal.DiffedLine
import io.github.taetae98coding.jarvis.domain.terminal.DockEdge
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.GitCommitFile
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitCommitFileUseCase
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.ImportChromeCookiesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsBrowserSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsChromeImportSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsClaudeSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveChromeProfilesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveClaudeActivitiesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveFileUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitFileDiffUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveGitWorktreeUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveTerminalWorkspaceUseCase
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.domain.terminal.UpdateTerminalWorkspaceUseCase
import io.github.taetae98coding.jarvis.domain.terminal.claudeSessionIds
import io.github.taetae98coding.jarvis.domain.terminal.claudeStatuses
import io.github.taetae98coding.jarvis.domain.terminal.newClaudeSessionId
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    isBrowserSupported: IsBrowserSupportedUseCase,
    isChromeImportSupported: IsChromeImportSupportedUseCase,
    private val observeChromeProfiles: ObserveChromeProfilesUseCase,
    private val importChromeCookies: ImportChromeCookiesUseCase,
    private val observeGitWorktree: ObserveGitWorktreeUseCase,
    private val worktreeTasks: WorktreeTaskHost,
    observeClaudeActivities: ObserveClaudeActivitiesUseCase,
    private val claudeAttention: ClaudeAttention,
    private val observeFile: ObserveFileUseCase,
    private val observeGitFileDiff: ObserveGitFileDiffUseCase,
    private val observeGitCommitFile: ObserveGitCommitFileUseCase,
    private val lineComments: LineCommentHost,
) : ViewModel() {
    val isClaudeSupported: Boolean = isClaudeSupported()

    val isBrowserSupported: Boolean = isBrowserSupported()

    val isChromeImportSupported: Boolean = isChromeImportSupported()

    // 페이지 제목은 저장하지 않는다. 엔진이 탭마다 제목을 알면(JVM) 그것을, 모르면(Android 의 웹뷰는 떠 있을 때만)
    // 마지막으로 본 제목을 보인다.
    private val browserTitles = mutableMapOf<Long, MutableStateFlow<String?>>()

    // 경로마다 하나. 같은 파일을 두 패널에서 열어도 디스크는 한 번만 따라간다.
    private val fileContents = mutableMapOf<String, StateFlow<FileContent?>>()

    private val fileDiffs = mutableMapOf<String, StateFlow<GitFileDiff?>>()

    // (경로, 해시)마다 하나. 커밋은 바뀌지 않으므로 탭이 보일 때마다 한 번 읽는다.
    private val commitFiles = mutableMapOf<CommitFileKey, StateFlow<GitCommitFile?>>()

    // 이 ViewModel 이 본 적 있는 탭. 사라진 것만 닫는다 — Claude 가 막 붙인 탭의 페이지를, 그 탭이 아직 없는
    // 옛 배치로 닫지 않게 한다(docs/common/mcp-server.html R5).
    private var knownTabIds: Set<Long> = emptySet()

    /** null 은 저장된 배치를 아직 읽지 못한 것이다. */
    val workspace: StateFlow<TerminalWorkspace?> = observeWorkspace()
        .onEach(::reconcile)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * 폴더가 git 저장소 안에 있는 패널마다 그 워크트리. 여기 없는 패널에는 + 가 없다. 패널 id·폴더 짝이 바뀔 때만
     * 다시 묶어서, 탭을 여닫는 것으로는 git 을 다시 읽지 않는다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val worktrees: StateFlow<Map<Long, GitWorktree>> = workspace
        .map { current -> current?.panels.orEmpty().mapNotNull { panel -> panel.directory?.let { panel.id to it } } }
        .distinctUntilChanged()
        .flatMapLatest { entries ->
            if (entries.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(entries.map { (id, directory) -> observeGitWorktree(directory).map { id to it } }) { pairs ->
                    pairs.mapNotNull { (id, worktree) -> worktree?.let { id to it } }.toMap()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    private val windowFocused = MutableStateFlow(false)

    /**
     * Claude 탭이 있는 패널마다 줄에 보일 탭별 상태. 조회는 Claude sessionId 집합이 바뀔 때만 다시 묶는다. 창이 포커스를
     * 가지면 보이는 탭의 끝난 결과를 확인한 것으로 계산하고 저장한다 — 계산에 먼저 반영해서 저장이 돌아오기 전
     * 한 프레임도 "응답 대기" 로 깜빡이지 않는다(docs/common/terminal-claude-status.html#implementation).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val claudeStatuses: StateFlow<Map<Long, List<ClaudeTabStatus>>> = combine(
        workspace.filterNotNull(),
        workspace
            .map { if (this.isClaudeSupported) it?.claudeSessionIds.orEmpty() else emptySet() }
            .distinctUntilChanged()
            .flatMapLatest { observeClaudeActivities(it) },
        windowFocused,
    ) { current, activities, focused ->
        val checked = if (focused) current.checkVisibleClaudeTabs(activities) else current
        if (checked !== current) update { it.checkVisibleClaudeTabs(activities) }

        checked.panels.mapNotNull { panel -> panel.claudeStatuses(activities).takeIf { it.isNotEmpty() }?.let { panel.id to it } }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    fun setWindowFocused(focused: Boolean) {
        windowFocused.value = focused
    }

    fun pane(tabId: Long): TerminalPaneState? = host.pane(tabId)

    /** 사용자가 지금 보고 있는 Claude 탭. 이 탭들은 턴이 끝나도 알리지 않는다. */
    fun watchClaude(sessionIds: Set<String>) = claudeAttention.watch(sessionIds)

    /** 셸 창은 셸이 정한 제목, 브라우저 탭은 페이지 제목. */
    fun title(tab: TerminalTab): StateFlow<String?>? =
        if (tab.program == TerminalProgram.Browser) browserTitle(tab.id) ?: viewedBrowserTitle(tab.id) else host.pane(tab.id)?.title

    fun setBrowserTitle(tabId: Long, title: String?) {
        viewedBrowserTitle(tabId).value = title
    }

    /** 파일 탭의 내용. null 은 아직 읽지 못한 것이다. 탭이 보이는 동안만 디스크를 따라간다. */
    fun fileContent(path: String): StateFlow<FileContent?> =
        fileContents.getOrPut(path) {
            observeFile(path).stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)
        }

    /** 파일 탭의 HEAD 대비 차이. null 은 아직 읽지 못했거나 저장소 밖이다. 탭이 보이는 동안만 git 을 따라간다. */
    fun fileDiff(path: String): StateFlow<GitFileDiff?> =
        fileDiffs.getOrPut(path) {
            observeGitFileDiff(path).stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)
        }

    fun openFile(path: String) = update { it.openFile(path) }

    /**
     * 커밋 파일 탭의 내용과 첫 부모 대비 diff(docs/common/terminal-commit-file.html). null 은 아직 읽지 못한 것이고, 읽지 못하는
     * 커밋·저장소 밖은 [FileContent.Unreadable] 에 빈 diff 다.
     */
    fun commitFile(path: String, hash: String): StateFlow<GitCommitFile?> =
        commitFiles.getOrPut(CommitFileKey(path, hash)) {
            observeGitCommitFile(path, hash)
                .map { it ?: GitCommitFile(FileContent.Unreadable, GitFileDiff(emptyList())) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)
        }

    fun openCommitFile(path: String, hash: String) = update { it.openCommitFile(path, hash) }

    /** 패널마다 모인 줄 코멘트(docs/common/terminal-line-comment.html). */
    val panelLineComments: StateFlow<Map<Long, PanelLineComments>> = lineComments.panels

    fun addLineComment(panelId: Long, path: String, lines: List<DiffedLine>, body: String) = lineComments.add(panelId, path, lines, body)

    fun removeLineComment(panelId: Long, commentId: Long) = lineComments.remove(panelId, commentId)

    fun clearLineComments(panelId: Long) = lineComments.clear(panelId)

    fun sendLineComments(panelId: Long, target: LineCommentTarget) = lineComments.send(panelId, target)

    private fun viewedBrowserTitle(tabId: Long): MutableStateFlow<String?> = browserTitles.getOrPut(tabId) { MutableStateFlow(null) }

    fun addPanel(name: String, directory: String) {
        val sessionId = firstClaudeSessionId()
        update { it.addPanel(name, directory, sessionId) }
    }

    val pendingWorktrees: StateFlow<List<PendingWorktree>> = worktreeTasks.pending

    val removingWorktreePanels: StateFlow<Set<Long>> = worktreeTasks.removing

    val worktreeFailures: StateFlow<List<WorktreeFailure>> = worktreeTasks.failures

    /** [parentId] 패널의 저장소에 워크트리를 만들고 그 아래 패널을 Claude 탭 하나로 붙이는 일을 뒤에서 시작한다. */
    fun addWorktreePanel(parentId: Long, parent: GitWorktree, branch: String, baseBranch: String?, directory: String) =
        worktreeTasks.add(parentId, parent, branch, baseBranch, directory, firstClaudeSessionId())

    // 새 패널의 첫 Claude 탭. Claude 를 띄울 수 없는 타깃이면 null 이고 패널은 빈 채로 시작한다.
    private fun firstClaudeSessionId(): String? = if (isClaudeSupported) newClaudeSessionId() else null

    fun renamePanel(panelId: Long, name: String) = update { it.renamePanel(panelId, name) }

    fun closePanel(panelId: Long) = update { it.closePanel(panelId) }

    /** [removeWorktree] 면 워크트리 패널의 워크트리·브랜치(와 [deleteDirectory] 면 폴더)를 지우고 패널을 닫는 일을 뒤에서 시작한다. */
    fun closeWorktreePanel(panelId: Long, worktree: GitWorktree, removeWorktree: Boolean, deleteDirectory: Boolean) =
        worktreeTasks.remove(panelId, worktree, removeWorktree, deleteDirectory)

    fun dismissWorktreeFailure(id: Long) = worktreeTasks.dismissFailure(id)

    fun selectPanel(panelId: Long) = update { it.selectPanel(panelId) }

    fun splitSideBySide() = update { it.split(SplitDirection.SideBySide, it.startDirectory()) }

    fun splitStacked() = update { it.split(SplitDirection.Stacked, it.startDirectory()) }

    /** [groupId] 가 null 이면 포커스된 그룹(없으면 새 그룹)이다. */
    fun addTab(groupId: Long? = null) = update { it.addTab(groupId, directory = it.startDirectory(groupId)) }

    fun addClaudeTab(groupId: Long? = null) {
        val sessionId = newClaudeSessionId()
        update { it.addTab(groupId, TerminalProgram.Claude, it.startDirectory(groupId), sessionId) }
    }

    fun addBrowserTab(groupId: Long? = null) =
        update { it.addTab(groupId, TerminalProgram.Browser, url = TerminalTab.DefaultBrowserUrl) }

    fun setUrl(tabId: Long, url: String) = update { it.setUrl(tabId, url) }

    fun addDeviceTab(groupId: Long?, choice: DeviceChoice) =
        update {
            it.addTab(
                groupId = groupId,
                program = TerminalProgram.Device,
                deviceId = choice.id,
                deviceName = choice.name,
                devicePlatform = choice.devicePlatform,
            )
        }

    /** 드롭다운을 열 때 지금 Chrome 프로필 목록을 읽는다(명령이 지금 값을 읽음). */
    suspend fun chromeProfiles(): List<ChromeProfile> = observeChromeProfiles().first()

    /** 고른 프로필의 모든 쿠키를 복호화해 돌려준다. 웹뷰에 넣는 것은 화면이 한다. */
    suspend fun importCookies(profileDirectory: String): List<BrowserCookie> = importChromeCookies(profileDirectory)

    fun closeFocusedTab() = update { it.closeFocusedTab() }

    fun closeTab(tabId: Long) = update { it.closeTab(tabId) }

    fun renameTab(tabId: Long, name: String) = update { it.renameTab(tabId, name) }

    fun selectTab(tabId: Long) = update { it.selectTab(tabId) }

    fun selectTabAt(index: Int) = update { it.selectTabAt(index) }

    fun selectAdjacentTab(offset: Int) = update { it.selectAdjacentTab(offset) }

    fun focusGroup(groupId: Long) = update { it.focusGroup(groupId) }

    fun focusAdjacentGroup(offset: Int) = update { it.focusAdjacentGroup(offset) }

    fun dockTab(tabId: Long, groupId: Long, edge: DockEdge) = update { it.dockTab(tabId, groupId, edge) }

    fun setRatio(splitId: Long, ratio: Float) = update { it.setRatio(splitId, ratio) }

    private fun update(transform: (TerminalWorkspace) -> TerminalWorkspace) {
        viewModelScope.launch {
            val change = updateWorkspace(transform)
            host.release(change.removedTabs.map { it.id })
        }
    }

    /**
     * 사라진 탭의 셸을 닫고, 지금 보이는 탭(선택된 패널에서 그룹마다 선택된 탭)의 창을 연다. 다른 탭·패널의
     * 창은 처음 보일 때 연다 — 앱을 켜자마자 모든 창을 띄우면 Claude 탭마다 백그라운드 세션을 찾는 셸이
     * 한꺼번에 돈다. 브라우저·기기 탭은 셸이 없어 열 창이 없다.
     */
    private fun reconcile(workspace: TerminalWorkspace) {
        val tabIds = workspace.tabIds.toSet()
        host.retain(tabIds)
        lineComments.retain(workspace.panels.mapTo(mutableSetOf()) { it.id })
        browserTitles.keys.retainAll(tabIds)
        val filePaths = workspace.tabs.mapNotNullTo(mutableSetOf()) { it.filePath }
        fileContents.keys.retainAll(filePaths)
        fileDiffs.keys.retainAll(filePaths)
        val commitFileKeys = workspace.tabs.mapNotNullTo(mutableSetOf()) { tab -> tab.commitHash?.let { hash -> tab.filePath?.let { CommitFileKey(it, hash) } } }
        commitFiles.keys.retainAll(commitFileKeys)
        closeBrowserPages(knownTabIds - tabIds)
        knownTabIds = tabIds

        val visible = workspace.visibleTabs.filter { it.program == TerminalProgram.Shell || it.program == TerminalProgram.Claude }

        // 새 창은 아직 배치되지 않았다. 이미 배치된 창의 크기로 먼저 띄우면 배치된 뒤의 크기와 가까워서,
        // 셸이 첫 프롬프트를 엉뚱한 너비로 그렸다가 다시 그리는 일이 줄어든다.
        val size = visible.firstNotNullOfOrNull { host.pane(it.id)?.emulator }
            ?.let { TerminalSize(it.columns, it.rows) }
            ?: TerminalSize.Default

        visible.forEach { host.acquire(it, size) }
    }
}

private data class CommitFileKey(val path: String, val hash: String)
