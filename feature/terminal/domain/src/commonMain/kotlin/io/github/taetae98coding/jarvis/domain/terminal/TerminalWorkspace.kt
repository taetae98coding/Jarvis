package io.github.taetae98coding.jarvis.domain.terminal

enum class SplitDirection {
    /** 좌우. iTerm 의 "Split Vertically"(⌘D). 새 창이 오른쪽에 온다. */
    SideBySide,

    /** 상하. iTerm 의 "Split Horizontally"(⌘⇧D). 새 창이 아래에 온다. */
    Stacked,
}

sealed interface PaneNode {
    /**
     * 창 하나. 다시 열 때 무엇을 띄울지를 들고 있다 — 앱을 다시 켜면 이 값만으로 창을 되살린다.
     *
     * [directory] 는 마지막으로 안 작업 디렉터리다. 모르면 null 이고 홈에서 시작한다.
     * [claudeSessionId] 는 [TerminalProgram.Claude] 창에만 있다.
     */
    data class Leaf(
        val paneId: Long,
        val program: TerminalProgram = TerminalProgram.Shell,
        val directory: String? = null,
        val claudeSessionId: String? = null,
    ) : PaneNode

    /** [ratio] 는 [first] 가 차지하는 몫이다. */
    data class Split(
        val id: Long,
        val direction: SplitDirection,
        val first: PaneNode,
        val second: PaneNode,
        val ratio: Float = 0.5f,
    ) : PaneNode
}

/** 화면 순서(왼쪽→오른쪽, 위→아래)대로의 창. */
val PaneNode.leaves: List<PaneNode.Leaf>
    get() = when (this) {
        is PaneNode.Leaf -> listOf(this)
        is PaneNode.Split -> first.leaves + second.leaves
    }

val PaneNode.paneIds: List<Long>
    get() = leaves.map { it.paneId }

data class TerminalTab(
    val id: Long,
    val root: PaneNode,
    val focusedPaneId: Long,
)

/** 왼쪽 목록의 한 줄. 탭은 만든 패널에 속하고 다른 패널로 옮겨 가지 않는다. */
data class TerminalPanel(
    val id: Long,
    val name: String,
    val tabs: List<TerminalTab>,
    val selectedTabId: Long?,
) {
    val selectedTab: TerminalTab?
        get() = tabs.firstOrNull { it.id == selectedTabId }

    val leaves: List<PaneNode.Leaf>
        get() = tabs.flatMap { it.root.leaves }
}

/**
 * 패널·탭·분할 트리. 셸은 들고 있지 않다 — 화면이 이 값의 창 id 와 자기가 띄운 셸을 맞춘다.
 *
 * 탭 단위 동작(분할, 탭 추가·선택, 포커스 이동)은 선택된 패널에 작용한다. 창·탭 id 로 가리키는
 * 동작(닫기, 포커스)은 그 id 가 있는 패널에 작용한다.
 *
 * 패널·탭·분할·창 id 는 모두 [nextId] 하나에서 나온다. 닫힌 창의 id 를 다시 쓰지 않아야 화면이
 * 새 창을 닫힌 창의 셸에 잇는 일이 없다. 저장했다가 다시 읽어도 [nextId] 가 함께 오므로 그대로다.
 */
data class TerminalWorkspace(
    val panels: List<TerminalPanel>,
    val selectedPanelId: Long?,
    val nextId: Long,
) {
    val selectedPanel: TerminalPanel?
        get() = panels.firstOrNull { it.id == selectedPanelId }

    val tabs: List<TerminalTab>
        get() = selectedPanel?.tabs.orEmpty()

    val selectedTabId: Long?
        get() = selectedPanel?.selectedTabId

    val selectedTab: TerminalTab?
        get() = selectedPanel?.selectedTab

    val focusedPaneId: Long?
        get() = selectedTab?.focusedPaneId

    val focusedLeaf: PaneNode.Leaf?
        get() = selectedTab?.let { tab -> tab.root.leaves.firstOrNull { it.paneId == tab.focusedPaneId } }

    /** 모든 패널의 창. */
    val leaves: List<PaneNode.Leaf>
        get() = panels.flatMap { it.leaves }

    val paneIds: List<Long>
        get() = leaves.map { it.paneId }

    /** 터미널 탭 하나를 가진 "패널 N" 을 끝에 붙이고 고른다. */
    fun addPanel(directory: String? = null): TerminalWorkspace {
        val panelId = nextId
        val tabId = nextId + 1
        val paneId = nextId + 2
        val panel = TerminalPanel(
            id = panelId,
            name = "$DefaultPanelName ${panels.size + 1}",
            tabs = listOf(TerminalTab(tabId, PaneNode.Leaf(paneId, directory = directory), paneId)),
            selectedTabId = tabId,
        )

        return copy(panels = panels + panel, selectedPanelId = panelId, nextId = nextId + 3)
    }

    /** 앞뒤 공백을 뗀다. 비어 있으면 바꾸지 않는다. */
    fun renamePanel(panelId: Long, name: String): TerminalWorkspace {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this

        return replacePanel(panelId) { it.copy(name = trimmed) }
    }

    /** 마지막 패널은 닫지 않는다. 선택된 패널이 닫히면 그 자리에 오는 패널(없으면 앞 패널)이 선택된다. */
    fun closePanel(panelId: Long): TerminalWorkspace {
        val index = panels.indexOfFirst { it.id == panelId }
        if (index < 0 || panels.size == 1) return this

        val remaining = panels.filterIndexed { i, _ -> i != index }
        val selected = if (selectedPanelId != panelId) {
            selectedPanelId
        } else {
            remaining[index.coerceAtMost(remaining.lastIndex)].id
        }

        return copy(panels = remaining, selectedPanelId = selected)
    }

    fun selectPanel(panelId: Long): TerminalWorkspace =
        if (panels.any { it.id == panelId }) copy(selectedPanelId = panelId) else this

    /** 선택된 패널의 탭 줄 끝에 창 하나짜리 탭을 붙이고 고른다. */
    fun addTab(
        program: TerminalProgram = TerminalProgram.Shell,
        directory: String? = null,
        claudeSessionId: String? = null,
    ): TerminalWorkspace {
        val panel = selectedPanel ?: return this
        val tabId = nextId
        val paneId = nextId + 1
        val leaf = PaneNode.Leaf(paneId, program, directory, claudeSessionId)

        return replacePanel(panel.id) {
            it.copy(tabs = it.tabs + TerminalTab(tabId, leaf, paneId), selectedTabId = tabId)
        }.copy(nextId = nextId + 2)
    }

    /** 새 창은 터미널이다. Claude 는 새 탭 메뉴로만 뜬다. */
    fun split(direction: SplitDirection, directory: String? = null): TerminalWorkspace {
        val tab = selectedTab ?: return this
        val splitId = nextId
        val paneId = nextId + 1

        val root = tab.root.replaceLeaf(tab.focusedPaneId) { leaf ->
            PaneNode.Split(
                id = splitId,
                direction = direction,
                first = leaf,
                second = PaneNode.Leaf(paneId, directory = directory),
            )
        }

        return replaceTab(tab.copy(root = root, focusedPaneId = paneId)).copy(nextId = nextId + 2)
    }

    /**
     * 형제가 부모 자리를 채운다. 닫힌 창이 포커스를 갖고 있었으면 형제 쪽에서 닫힌 자리와 맞닿은
     * 창이 포커스를 받는다. 탭의 마지막 창이면 탭이 닫힌다.
     */
    fun closePane(paneId: Long): TerminalWorkspace {
        val tab = findTab { paneId in it.root.paneIds } ?: return this
        val removal = tab.root.remove(paneId) ?: return closeTab(tab.id)

        val focused = if (tab.focusedPaneId == paneId) removal.neighbor else tab.focusedPaneId

        return replaceTab(tab.copy(root = removal.root, focusedPaneId = focused))
    }

    /** 선택된 탭이 닫히면 그 자리에 오는 탭(없으면 앞 탭)이 선택된다. 마지막 탭이면 패널이 빈 채로 남는다. */
    fun closeTab(tabId: Long): TerminalWorkspace {
        val panel = panels.firstOrNull { panel -> panel.tabs.any { it.id == tabId } } ?: return this
        val index = panel.tabs.indexOfFirst { it.id == tabId }

        val remaining = panel.tabs.filterIndexed { i, _ -> i != index }
        val selected = when {
            panel.selectedTabId != tabId -> panel.selectedTabId
            remaining.isEmpty() -> null
            else -> remaining[index.coerceAtMost(remaining.lastIndex)].id
        }

        return replacePanel(panel.id) { it.copy(tabs = remaining, selectedTabId = selected) }
    }

    /** 다른 패널의 탭이면 그 패널도 함께 고른다. */
    fun selectTab(tabId: Long): TerminalWorkspace {
        val panel = panels.firstOrNull { panel -> panel.tabs.any { it.id == tabId } } ?: return this

        return replacePanel(panel.id) { it.copy(selectedTabId = tabId) }.copy(selectedPanelId = panel.id)
    }

    fun selectTabAt(index: Int): TerminalWorkspace =
        tabs.getOrNull(index)?.let { selectTab(it.id) } ?: this

    /** 끝에서 넘어가면 반대쪽 끝으로 돈다. */
    fun selectAdjacentTab(offset: Int): TerminalWorkspace {
        val index = tabs.indexOfFirst { it.id == selectedTabId }
        if (index < 0) return this

        return selectTabAt((index + offset).mod(tabs.size))
    }

    /** 다른 탭·패널의 창이면 그 탭과 패널도 함께 고른다. */
    fun focusPane(paneId: Long): TerminalWorkspace {
        val tab = findTab { paneId in it.root.paneIds } ?: return this

        return replaceTab(tab.copy(focusedPaneId = paneId)).selectTab(tab.id)
    }

    fun focusAdjacentPane(offset: Int): TerminalWorkspace {
        val tab = selectedTab ?: return this
        val ids = tab.root.paneIds
        val index = ids.indexOf(tab.focusedPaneId)

        return focusPane(ids[(index + offset).mod(ids.size)])
    }

    fun setRatio(splitId: Long, ratio: Float): TerminalWorkspace {
        val tab = findTab { it.root.findSplit(splitId) != null } ?: return this

        return replaceTab(tab.copy(root = tab.root.withRatio(splitId, ratio.coerceIn(MinRatio, MaxRatio))))
    }

    /** 경계선을 끄는 동안 한 프레임에 여러 번 불린다. 매번 지금 값에 더해야 움직임을 잃지 않는다. */
    fun resizeSplit(splitId: Long, delta: Float): TerminalWorkspace {
        val split = panels.firstNotNullOfOrNull { panel -> panel.tabs.firstNotNullOfOrNull { it.root.findSplit(splitId) } }
            ?: return this

        return setRatio(splitId, split.ratio + delta)
    }

    fun setDirectory(paneId: Long, directory: String): TerminalWorkspace {
        val tab = findTab { paneId in it.root.paneIds } ?: return this

        return replaceTab(tab.copy(root = tab.root.replaceLeaf(paneId) { it.copy(directory = directory) }))
    }

    private fun findTab(predicate: (TerminalTab) -> Boolean): TerminalTab? =
        panels.firstNotNullOfOrNull { panel -> panel.tabs.firstOrNull(predicate) }

    private fun replacePanel(panelId: Long, transform: (TerminalPanel) -> TerminalPanel): TerminalWorkspace =
        copy(panels = panels.map { if (it.id == panelId) transform(it) else it })

    private fun replaceTab(tab: TerminalTab): TerminalWorkspace =
        copy(
            panels = panels.map { panel ->
                if (panel.tabs.none { it.id == tab.id }) panel else panel.copy(tabs = panel.tabs.map { if (it.id == tab.id) tab else it })
            },
        )

    companion object {
        const val MinRatio = 0.1f
        const val MaxRatio = 0.9f

        const val DefaultPanelName = "패널"

        /** 처음 켰을 때. 패널 하나, 탭 하나, 창 하나. */
        fun initial(): TerminalWorkspace =
            TerminalWorkspace(panels = emptyList(), selectedPanelId = null, nextId = 1).addPanel()
    }
}

private fun PaneNode.replaceLeaf(paneId: Long, transform: (PaneNode.Leaf) -> PaneNode): PaneNode =
    when (this) {
        is PaneNode.Leaf -> if (this.paneId == paneId) transform(this) else this
        is PaneNode.Split -> copy(
            first = first.replaceLeaf(paneId, transform),
            second = second.replaceLeaf(paneId, transform),
        )
    }

private class Removal(val root: PaneNode, val neighbor: Long)

private fun PaneNode.isLeaf(paneId: Long): Boolean = this is PaneNode.Leaf && this.paneId == paneId

// 첫째 자식이 빠지면 둘째의 맨 앞 창이, 둘째가 빠지면 첫째의 맨 뒤 창이 닫힌 자리와 맞닿아 있다.
private fun PaneNode.remove(paneId: Long): Removal? =
    when (this) {
        is PaneNode.Leaf -> null

        is PaneNode.Split -> when {
            first.isLeaf(paneId) -> Removal(second, second.paneIds.first())
            second.isLeaf(paneId) -> Removal(first, first.paneIds.last())
            paneId in first.paneIds -> first.remove(paneId)?.let { Removal(copy(first = it.root), it.neighbor) }
            else -> second.remove(paneId)?.let { Removal(copy(second = it.root), it.neighbor) }
        }
    }

private fun PaneNode.findSplit(splitId: Long): PaneNode.Split? =
    when (this) {
        is PaneNode.Leaf -> null
        is PaneNode.Split -> if (id == splitId) this else first.findSplit(splitId) ?: second.findSplit(splitId)
    }

private fun PaneNode.withRatio(splitId: Long, ratio: Float): PaneNode =
    when (this) {
        is PaneNode.Leaf -> this
        is PaneNode.Split -> if (id == splitId) {
            copy(ratio = ratio)
        } else {
            copy(first = first.withRatio(splitId, ratio), second = second.withRatio(splitId, ratio))
        }
    }
