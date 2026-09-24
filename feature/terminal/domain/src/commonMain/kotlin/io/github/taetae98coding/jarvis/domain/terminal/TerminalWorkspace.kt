package io.github.taetae98coding.jarvis.domain.terminal

enum class SplitDirection {
    /** 좌우. iTerm 의 "Split Vertically"(⌘D). 새 패널이 오른쪽에 온다. */
    SideBySide,

    /** 상하. iTerm 의 "Split Horizontally"(⌘⇧D). 새 패널이 아래에 온다. */
    Stacked,
}

sealed interface PaneNode {
    data class Leaf(val paneId: Long) : PaneNode

    /** [ratio] 는 [first] 가 차지하는 몫이다. */
    data class Split(
        val id: Long,
        val direction: SplitDirection,
        val first: PaneNode,
        val second: PaneNode,
        val ratio: Float = 0.5f,
    ) : PaneNode
}

/** 화면 순서(왼쪽→오른쪽, 위→아래)대로의 패널 id. */
val PaneNode.paneIds: List<Long>
    get() = when (this) {
        is PaneNode.Leaf -> listOf(paneId)
        is PaneNode.Split -> first.paneIds + second.paneIds
    }

data class TerminalTab(
    val id: Long,
    val root: PaneNode,
    val focusedPaneId: Long,
)

/**
 * 탭과 분할 트리. 셸은 들고 있지 않다 — 화면이 이 값의 패널 id 목록과 자기가 띄운 셸을 맞춘다.
 *
 * 탭·분할·패널 id 는 모두 [nextId] 하나에서 나온다. 닫힌 패널의 id 를 다시 쓰지 않아야 화면이
 * 새 패널을 닫힌 패널의 셸에 잇는 일이 없다.
 */
data class TerminalWorkspace(
    val tabs: List<TerminalTab>,
    val selectedTabId: Long?,
    val nextId: Long,
) {
    val selectedTab: TerminalTab?
        get() = tabs.firstOrNull { it.id == selectedTabId }

    val focusedPaneId: Long?
        get() = selectedTab?.focusedPaneId

    val paneIds: List<Long>
        get() = tabs.flatMap { it.root.paneIds }

    fun addTab(): TerminalWorkspace {
        val tabId = nextId
        val paneId = nextId + 1

        return copy(
            tabs = tabs + TerminalTab(id = tabId, root = PaneNode.Leaf(paneId), focusedPaneId = paneId),
            selectedTabId = tabId,
            nextId = nextId + 2,
        )
    }

    fun split(direction: SplitDirection): TerminalWorkspace {
        val tab = selectedTab ?: return this
        val splitId = nextId
        val paneId = nextId + 1

        val root = tab.root.replaceLeaf(tab.focusedPaneId) { leaf ->
            PaneNode.Split(id = splitId, direction = direction, first = leaf, second = PaneNode.Leaf(paneId))
        }

        return replace(tab.copy(root = root, focusedPaneId = paneId)).copy(nextId = nextId + 2)
    }

    /**
     * 형제가 부모 자리를 채운다. 닫힌 패널이 포커스를 갖고 있었으면 형제 쪽에서 닫힌 자리와 맞닿은
     * 패널이 포커스를 받는다. 탭의 마지막 패널이면 탭이 닫힌다.
     */
    fun closePane(paneId: Long): TerminalWorkspace {
        val tab = tabs.firstOrNull { paneId in it.root.paneIds } ?: return this
        val removal = tab.root.remove(paneId) ?: return closeTab(tab.id)

        val focused = if (tab.focusedPaneId == paneId) removal.neighbor else tab.focusedPaneId

        return replace(tab.copy(root = removal.root, focusedPaneId = focused))
    }

    /** 선택된 탭이 닫히면 그 자리에 오는 탭(없으면 앞 탭)이 선택된다. */
    fun closeTab(tabId: Long): TerminalWorkspace {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return this

        val remaining = tabs.filterIndexed { i, _ -> i != index }
        val selected = when {
            selectedTabId != tabId -> selectedTabId
            remaining.isEmpty() -> null
            else -> remaining[index.coerceAtMost(remaining.lastIndex)].id
        }

        return copy(tabs = remaining, selectedTabId = selected)
    }

    fun selectTab(tabId: Long): TerminalWorkspace =
        if (tabs.any { it.id == tabId }) copy(selectedTabId = tabId) else this

    fun selectTabAt(index: Int): TerminalWorkspace =
        tabs.getOrNull(index)?.let { selectTab(it.id) } ?: this

    /** 끝에서 넘어가면 반대쪽 끝으로 돈다. */
    fun selectAdjacentTab(offset: Int): TerminalWorkspace {
        val index = tabs.indexOfFirst { it.id == selectedTabId }
        if (index < 0) return this

        return selectTabAt((index + offset).mod(tabs.size))
    }

    /** 다른 탭의 패널이면 그 탭도 함께 고른다. */
    fun focusPane(paneId: Long): TerminalWorkspace {
        val tab = tabs.firstOrNull { paneId in it.root.paneIds } ?: return this

        return replace(tab.copy(focusedPaneId = paneId)).copy(selectedTabId = tab.id)
    }

    fun focusAdjacentPane(offset: Int): TerminalWorkspace {
        val tab = selectedTab ?: return this
        val ids = tab.root.paneIds
        val index = ids.indexOf(tab.focusedPaneId)

        return focusPane(ids[(index + offset).mod(ids.size)])
    }

    fun setRatio(splitId: Long, ratio: Float): TerminalWorkspace {
        val tab = tabs.firstOrNull { it.root.findSplit(splitId) != null } ?: return this

        return replace(tab.copy(root = tab.root.withRatio(splitId, ratio.coerceIn(MinRatio, MaxRatio))))
    }

    /** 경계선을 끄는 동안 한 프레임에 여러 번 불린다. 매번 지금 값에 더해야 움직임을 잃지 않는다. */
    fun resizeSplit(splitId: Long, delta: Float): TerminalWorkspace {
        val split = tabs.firstNotNullOfOrNull { it.root.findSplit(splitId) } ?: return this

        return setRatio(splitId, split.ratio + delta)
    }

    private fun replace(tab: TerminalTab): TerminalWorkspace =
        copy(tabs = tabs.map { if (it.id == tab.id) tab else it })

    companion object {
        const val MinRatio = 0.1f
        const val MaxRatio = 0.9f

        /** 화면에 처음 들어왔을 때. 탭 하나, 패널 하나. */
        fun initial(): TerminalWorkspace =
            TerminalWorkspace(tabs = emptyList(), selectedTabId = null, nextId = 1).addTab()
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

// 첫째 자식이 빠지면 둘째의 맨 앞 패널이, 둘째가 빠지면 첫째의 맨 뒤 패널이 닫힌 자리와 맞닿아 있다.
private fun PaneNode.remove(paneId: Long): Removal? =
    when (this) {
        is PaneNode.Leaf -> null

        is PaneNode.Split -> when {
            first == PaneNode.Leaf(paneId) -> Removal(second, second.paneIds.first())
            second == PaneNode.Leaf(paneId) -> Removal(first, first.paneIds.last())
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
