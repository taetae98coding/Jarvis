package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.AddWorktreePanelUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeTabStatus
import io.github.taetae98coding.jarvis.domain.terminal.CloseWorktreePanelUseCase
import io.github.taetae98coding.jarvis.domain.terminal.DockEdge
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.ImportChromeCookiesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsBrowserSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsChromeImportSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.IsClaudeSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveChromeProfilesUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveClaudeActivitiesUseCase
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
import kotlinx.coroutines.async
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
    private val addWorktree: AddWorktreePanelUseCase,
    private val closeWorktree: CloseWorktreePanelUseCase,
    observeClaudeActivities: ObserveClaudeActivitiesUseCase,
    private val claudeAttention: ClaudeAttention,
) : ViewModel() {
    val isClaudeSupported: Boolean = isClaudeSupported()

    val isBrowserSupported: Boolean = isBrowserSupported()

    val isChromeImportSupported: Boolean = isChromeImportSupported()

    // 페이지 제목은 웹뷰가 떠 있을 때만 알 수 있다. 저장하지 않고, 가려진 탭은 마지막으로 본 제목을 보인다.
    private val browserTitles = mutableMapOf<Long, MutableStateFlow<String?>>()

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
        if (tab.program == TerminalProgram.Browser) browserTitle(tab.id) else host.pane(tab.id)?.title

    fun setBrowserTitle(tabId: Long, title: String?) {
        browserTitle(tabId).value = title
    }

    private fun browserTitle(tabId: Long): MutableStateFlow<String?> = browserTitles.getOrPut(tabId) { MutableStateFlow(null) }

    fun addPanel(name: String, directory: String) = update { it.addPanel(name, directory) }

    /**
     * [parentId] 패널의 저장소에 워크트리를 만들고 그 아래 빈 패널을 붙인다. 창이 닫혀 이 호출이 취소돼도 git 명령과
     * 패널 추가는 끝까지 간다 — 만들다 만 워크트리가 패널 없이 남지 않게.
     */
    suspend fun addWorktreePanel(parentId: Long, branch: String, baseBranch: String?, directory: String): Result<Unit> =
        viewModelScope.async { addWorktree(parentId, branch, baseBranch, directory).map { } }.await()

    fun renamePanel(panelId: Long, name: String) = update { it.renamePanel(panelId, name) }

    fun closePanel(panelId: Long) = update { it.closePanel(panelId) }

    /**
     * [removeWorktree] 면 워크트리 패널의 워크트리·브랜치(와 [deleteDirectory] 면 폴더)를 지우고 패널을 닫는다. 창이 닫혀
     * 취소돼도 끝까지 간다 — 지워진 워크트리를 가리키는 패널이 남지 않게.
     */
    suspend fun closeWorktreePanel(panelId: Long, removeWorktree: Boolean, deleteDirectory: Boolean): Result<Unit> =
        viewModelScope.async {
            closeWorktree(panelId, removeWorktree, deleteDirectory).map { change -> host.release(change.removedTabs.map { it.id }) }
        }.await()

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
        browserTitles.keys.retainAll(tabIds)

        val visible = workspace.visibleTabs.filter { it.program == TerminalProgram.Shell || it.program == TerminalProgram.Claude }

        // 새 창은 아직 배치되지 않았다. 이미 배치된 창의 크기로 먼저 띄우면 배치된 뒤의 크기와 가까워서,
        // 셸이 첫 프롬프트를 엉뚱한 너비로 그렸다가 다시 그리는 일이 줄어든다.
        val size = visible.firstNotNullOfOrNull { host.pane(it.id)?.emulator }
            ?.let { TerminalSize(it.columns, it.rows) }
            ?: TerminalSize.Default

        visible.forEach { host.acquire(it, size) }
    }
}
