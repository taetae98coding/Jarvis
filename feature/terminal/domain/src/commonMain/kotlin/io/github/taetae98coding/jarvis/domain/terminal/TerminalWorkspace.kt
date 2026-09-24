package io.github.taetae98coding.jarvis.domain.terminal

enum class SplitDirection {
    /** 좌우. iTerm 의 "Split Vertically"(⌘D). 새 그룹이 오른쪽에 온다. */
    SideBySide,

    /** 상하. iTerm 의 "Split Horizontally"(⌘⇧D). 새 그룹이 아래에 온다. */
    Stacked,
}

/** 탭을 끌어다 놓는 그룹의 자리. 변이면 그 방향으로 나뉘고, [Center] 는 나누지 않고 그 그룹에 넣는다. */
enum class DockEdge(val splitDirection: SplitDirection?, val placesFirst: Boolean) {
    Left(SplitDirection.SideBySide, placesFirst = true),
    Right(SplitDirection.SideBySide, placesFirst = false),
    Top(SplitDirection.Stacked, placesFirst = true),
    Bottom(SplitDirection.Stacked, placesFirst = false),
    Center(null, placesFirst = false),
}

/**
 * 탭 하나 = 창 하나. 다시 열 때 무엇을 띄울지를 들고 있다 — 앱을 다시 켜면 이 값만으로 창을 되살린다.
 *
 * [directory] 는 마지막으로 안 작업 디렉터리다. 모르면 null 이고 홈에서 시작한다.
 * [claudeSessionId] 는 [TerminalProgram.Claude] 탭에만, [url] 은 [TerminalProgram.Browser] 탭에만 있다.
 */
data class TerminalTab(
    val id: Long,
    val program: TerminalProgram = TerminalProgram.Shell,
    val directory: String? = null,
    val claudeSessionId: String? = null,
    val url: String? = null,
) {
    companion object {
        const val DefaultBrowserUrl = "https://www.google.com"
    }
}

sealed interface PaneNode {
    /** 나뉜 칸 하나. 자기 탭 줄을 가진다. [tabs] 는 비지 않는다 — 마지막 탭이 닫히면 그룹이 사라진다. */
    data class Group(
        val id: Long,
        val tabs: List<TerminalTab>,
        val selectedTabId: Long,
    ) : PaneNode {
        val selectedTab: TerminalTab
            get() = tabs.firstOrNull { it.id == selectedTabId } ?: tabs.first()
    }

    /** [ratio] 는 [first] 가 차지하는 몫이다. */
    data class Split(
        val id: Long,
        val direction: SplitDirection,
        val first: PaneNode,
        val second: PaneNode,
        val ratio: Float = 0.5f,
    ) : PaneNode
}

/** 화면 순서(왼쪽→오른쪽, 위→아래)대로의 그룹. */
val PaneNode.groups: List<PaneNode.Group>
    get() = when (this) {
        is PaneNode.Group -> listOf(this)
        is PaneNode.Split -> first.groups + second.groups
    }

val PaneNode.tabs: List<TerminalTab>
    get() = groups.flatMap { it.tabs }

/**
 * 왼쪽 목록의 한 줄. [root] 가 null 이면 그룹이 없는 빈 패널이다. 탭은 만든 패널에 속하고 다른 패널로 옮겨 가지 않는다.
 *
 * [directory] 는 만들 때 정한 폴더다. 탭의 작업 디렉터리를 모를 때 새 탭이 여기서 시작한다.
 */
data class TerminalPanel(
    val id: Long,
    val name: String,
    val root: PaneNode?,
    val focusedGroupId: Long?,
    val directory: String? = null,
) {
    val groups: List<PaneNode.Group>
        get() = root?.groups.orEmpty()

    val tabs: List<TerminalTab>
        get() = groups.flatMap { it.tabs }

    /** 저장된 포커스가 사라진 그룹을 가리키면 첫 그룹이다. */
    val focusedGroup: PaneNode.Group?
        get() = groups.firstOrNull { it.id == focusedGroupId } ?: groups.firstOrNull()
}

/**
 * 패널·그룹·탭 배치. 셸은 들고 있지 않다 — 화면이 이 값의 탭 id 와 자기가 띄운 셸을 맞춘다.
 *
 * 그룹 단위 동작(분할, 탭 추가, 포커스 이동)은 선택된 패널의 포커스된 그룹에 작용한다. 탭·그룹 id 로
 * 가리키는 동작(닫기, 선택, 포커스, 끌어 놓기)은 그 id 가 있는 패널에 작용한다.
 *
 * 패널·그룹·분할·탭 id 는 모두 [nextId] 하나에서 나온다. 닫힌 탭의 id 를 다시 쓰지 않아야 화면이
 * 새 탭을 닫힌 탭의 셸에 잇는 일이 없다. 저장했다가 다시 읽어도 [nextId] 가 함께 오므로 그대로다.
 */
data class TerminalWorkspace(
    val panels: List<TerminalPanel>,
    val selectedPanelId: Long?,
    val nextId: Long,
) {
    val selectedPanel: TerminalPanel?
        get() = panels.firstOrNull { it.id == selectedPanelId }

    /** 선택된 패널의 그룹. 화면 순서다. */
    val groups: List<PaneNode.Group>
        get() = selectedPanel?.groups.orEmpty()

    val focusedGroup: PaneNode.Group?
        get() = selectedPanel?.focusedGroup

    val focusedTab: TerminalTab?
        get() = focusedGroup?.selectedTab

    /** 선택된 패널에서 지금 보이는 탭. 그룹마다 선택된 탭 하나다. */
    val visibleTabs: List<TerminalTab>
        get() = groups.map { it.selectedTab }

    /** 모든 패널의 탭. */
    val tabs: List<TerminalTab>
        get() = panels.flatMap { it.tabs }

    val tabIds: List<Long>
        get() = tabs.map { it.id }

    /** [addPanel] 에 이름을 주지 않았을 때의 이름. */
    val nextPanelName: String
        get() = "$DefaultPanelName ${panels.size + 1}"

    /**
     * 탭 하나짜리 그룹을 가진 패널을 끝에 붙이고 고른다. [name]·[directory] 는 앞뒤 공백을 떼고, 비면
     * [nextPanelName]·폴더 없음이다. 첫 탭은 패널 폴더에서 시작한다.
     */
    fun addPanel(
        name: String? = null,
        directory: String? = null,
        program: TerminalProgram = TerminalProgram.Shell,
        claudeSessionId: String? = null,
    ): TerminalWorkspace {
        val panelId = nextId
        val groupId = nextId + 1
        val tabId = nextId + 2
        val folder = directory?.trim()?.ifEmpty { null }
        val panel = TerminalPanel(
            id = panelId,
            name = name?.trim()?.ifEmpty { null } ?: nextPanelName,
            root = PaneNode.Group(groupId, listOf(TerminalTab(tabId, program, folder, claudeSessionId)), tabId),
            focusedGroupId = groupId,
            directory = folder,
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

    /**
     * [groupId] 그룹(null 이면 선택된 패널의 포커스된 그룹)의 탭 줄 끝에 탭을 붙이고 고른다. 그 그룹이
     * 포커스를 받는다. 패널에 그룹이 없으면 그룹을 하나 만들어 넣는다.
     */
    fun addTab(
        groupId: Long? = null,
        program: TerminalProgram = TerminalProgram.Shell,
        directory: String? = null,
        claudeSessionId: String? = null,
        url: String? = null,
    ): TerminalWorkspace {
        val panel = (if (groupId == null) selectedPanel else findPanel { panel -> panel.groups.any { it.id == groupId } })
            ?: return this
        val group = if (groupId == null) panel.focusedGroup else panel.groups.first { it.id == groupId }
        val tabId = nextId
        val tab = TerminalTab(tabId, program, directory, claudeSessionId, url)

        if (group == null) {
            val newGroupId = nextId + 1

            return replacePanel(panel.id) { it.copy(root = PaneNode.Group(newGroupId, listOf(tab), tabId), focusedGroupId = newGroupId) }
                .copy(nextId = nextId + 2)
        }

        return replaceGroup(group.id) { it.copy(tabs = it.tabs + tab, selectedTabId = tabId) }
            .focusGroup(group.id)
            .copy(nextId = nextId + 1)
    }

    /** 포커스된 그룹을 나눠 셸 탭 하나짜리 새 그룹을 오른쪽·아래에 두고 포커스한다. Claude·브라우저는 새 탭 메뉴로만 뜬다. */
    fun split(direction: SplitDirection, directory: String? = null): TerminalWorkspace {
        val group = focusedGroup ?: return this
        val splitId = nextId
        val groupId = nextId + 1
        val tabId = nextId + 2
        val added = PaneNode.Group(groupId, listOf(TerminalTab(tabId, directory = directory)), tabId)

        return replaceGroup(group.id) { PaneNode.Split(splitId, direction, first = it, second = added) }
            .focusGroup(groupId)
            .copy(nextId = nextId + 3)
    }

    /**
     * 선택된 탭이 닫히면 그 자리에 오는 탭(없으면 앞 탭)이 선택된다. 그룹의 마지막 탭이면 그룹이 사라지고
     * 형제가 부모 자리를 채우며, 사라진 그룹이 포커스를 갖고 있었으면 닫힌 자리와 맞닿은 그룹이 포커스를
     * 받는다. 패널의 마지막 그룹이면 패널이 빈 채로 남는다.
     */
    fun closeTab(tabId: Long): TerminalWorkspace {
        val group = findGroup { group -> group.tabs.any { it.id == tabId } } ?: return this
        if (group.tabs.size == 1) return removeGroup(group.id)

        val index = group.tabs.indexOfFirst { it.id == tabId }
        val remaining = group.tabs.filterIndexed { i, _ -> i != index }
        val selected = if (group.selectedTabId != tabId) group.selectedTabId else remaining[index.coerceAtMost(remaining.lastIndex)].id

        return replaceGroup(group.id) { it.copy(tabs = remaining, selectedTabId = selected) }
    }

    fun closeFocusedTab(): TerminalWorkspace = focusedTab?.let { closeTab(it.id) } ?: this

    /** 탭을 자기 그룹에서 고르고 그 그룹과 패널에 포커스·선택을 준다. */
    fun selectTab(tabId: Long): TerminalWorkspace {
        val group = findGroup { group -> group.tabs.any { it.id == tabId } } ?: return this

        return replaceGroup(group.id) { it.copy(selectedTabId = tabId) }.focusGroup(group.id)
    }

    /** 포커스된 그룹 안에서 [index] 번째 탭. */
    fun selectTabAt(index: Int): TerminalWorkspace =
        focusedGroup?.tabs?.getOrNull(index)?.let { selectTab(it.id) } ?: this

    /** 포커스된 그룹 안에서. 끝에서 넘어가면 반대쪽 끝으로 돈다. */
    fun selectAdjacentTab(offset: Int): TerminalWorkspace {
        val group = focusedGroup ?: return this
        val index = group.tabs.indexOfFirst { it.id == group.selectedTabId }

        return selectTabAt((index + offset).mod(group.tabs.size))
    }

    /** 다른 패널의 그룹이면 그 패널도 함께 고른다. */
    fun focusGroup(groupId: Long): TerminalWorkspace {
        val panel = findPanel { panel -> panel.groups.any { it.id == groupId } } ?: return this

        return replacePanel(panel.id) { it.copy(focusedGroupId = groupId) }.copy(selectedPanelId = panel.id)
    }

    /** 선택된 패널의 화면 순서로. 끝에서 넘어가면 반대쪽 끝으로 돈다. */
    fun focusAdjacentGroup(offset: Int): TerminalWorkspace {
        val focused = focusedGroup ?: return this
        val index = groups.indexOfFirst { it.id == focused.id }

        return focusGroup(groups[(index + offset).mod(groups.size)].id)
    }

    /**
     * [tabId] 탭을 [groupId] 그룹의 [edge] 에 놓는다. 변이면 그 그룹이 나뉘어 그 탭 하나짜리 새 그룹이 그
     * 변 쪽에 생기고, [DockEdge.Center] 면 그 그룹의 탭 줄 끝에 들어간다. 탭은 원래 그룹에서 빠지고 그
     * 그룹이 비면 사라진다. 놓아도 배치가 같은 자리(자기 그룹의 가운데, 탭 하나뿐인 자기 그룹의 변)와
     * 모르는 id 는 그대로다. 탭이 사라지지 않으므로 세션은 하나도 닫히지 않는다.
     */
    fun dockTab(tabId: Long, groupId: Long, edge: DockEdge): TerminalWorkspace {
        val source = findGroup { group -> group.tabs.any { it.id == tabId } } ?: return this
        val target = findGroup { it.id == groupId } ?: return this
        if (source.id == target.id && (edge == DockEdge.Center || source.tabs.size == 1)) return this

        val tab = source.tabs.first { it.id == tabId }
        val detached = closeTab(tabId)
        val direction = edge.splitDirection
            ?: return detached.replaceGroup(target.id) { it.copy(tabs = it.tabs + tab, selectedTabId = tabId) }.focusGroup(target.id)

        val splitId = nextId
        val newGroupId = nextId + 1
        val added = PaneNode.Group(newGroupId, listOf(tab), tabId)

        return detached
            .replaceGroup(target.id) { group ->
                PaneNode.Split(
                    id = splitId,
                    direction = direction,
                    first = if (edge.placesFirst) added else group,
                    second = if (edge.placesFirst) group else added,
                )
            }
            .focusGroup(newGroupId)
            .copy(nextId = nextId + 2)
    }

    fun setRatio(splitId: Long, ratio: Float): TerminalWorkspace {
        val panel = findPanel { it.root?.findSplit(splitId) != null } ?: return this

        return replacePanel(panel.id) { it.copy(root = it.root?.withRatio(splitId, ratio.coerceIn(MinRatio, MaxRatio))) }
    }

    /** 경계선을 끄는 동안 한 프레임에 여러 번 불린다. 매번 지금 값에 더해야 움직임을 잃지 않는다. */
    fun resizeSplit(splitId: Long, delta: Float): TerminalWorkspace {
        val split = panels.firstNotNullOfOrNull { it.root?.findSplit(splitId) } ?: return this

        return setRatio(splitId, split.ratio + delta)
    }

    /**
     * [groupId] 그룹(null 이면 포커스된 그룹)에 새로 여는 탭의 시작 디렉터리. 그 그룹에서 선택된 탭의
     * 디렉터리, 모르면 선택된 패널의 폴더다. null 이면 홈이다.
     */
    fun startDirectory(groupId: Long? = null): String? {
        val group = if (groupId == null) focusedGroup else groups.firstOrNull { it.id == groupId }

        return group?.selectedTab?.directory ?: selectedPanel?.directory
    }

    fun setDirectory(tabId: Long, directory: String): TerminalWorkspace = replaceTab(tabId) { it.copy(directory = directory) }

    fun setUrl(tabId: Long, url: String): TerminalWorkspace = replaceTab(tabId) { it.copy(url = url) }

    /**
     * 형제가 부모 자리를 채운다. 사라진 그룹이 포커스를 갖고 있었으면 형제 쪽에서 닫힌 자리와 맞닿은
     * 그룹이 포커스를 받는다. 패널의 마지막 그룹이면 패널이 빈다.
     */
    private fun removeGroup(groupId: Long): TerminalWorkspace {
        val panel = findPanel { panel -> panel.groups.any { it.id == groupId } } ?: return this
        val root = panel.root ?: return this

        if (root is PaneNode.Group) return replacePanel(panel.id) { it.copy(root = null, focusedGroupId = null) }

        val removal = root.remove(groupId) ?: return this
        val focused = if (panel.focusedGroupId == groupId) removal.neighbor else panel.focusedGroupId

        return replacePanel(panel.id) { it.copy(root = removal.root, focusedGroupId = focused) }
    }

    // 같은 값이면 자신을 돌려준다. 저장소가 바뀌지 않은 값을 다시 쓰지 않는다.
    private fun replaceTab(tabId: Long, transform: (TerminalTab) -> TerminalTab): TerminalWorkspace {
        val group = findGroup { group -> group.tabs.any { it.id == tabId } } ?: return this
        val tabs = group.tabs.map { if (it.id == tabId) transform(it) else it }
        if (tabs == group.tabs) return this

        return replaceGroup(group.id) { it.copy(tabs = tabs) }
    }

    private fun findPanel(predicate: (TerminalPanel) -> Boolean): TerminalPanel? = panels.firstOrNull(predicate)

    private fun findGroup(predicate: (PaneNode.Group) -> Boolean): PaneNode.Group? =
        panels.firstNotNullOfOrNull { panel -> panel.groups.firstOrNull(predicate) }

    private fun replacePanel(panelId: Long, transform: (TerminalPanel) -> TerminalPanel): TerminalWorkspace =
        copy(panels = panels.map { if (it.id == panelId) transform(it) else it })

    private fun replaceGroup(groupId: Long, transform: (PaneNode.Group) -> PaneNode): TerminalWorkspace =
        copy(panels = panels.map { panel -> panel.copy(root = panel.root?.replaceGroup(groupId, transform)) })

    companion object {
        const val MinRatio = 0.1f
        const val MaxRatio = 0.9f

        const val DefaultPanelName = "패널"

        /** 처음 켰을 때. 패널 하나, 그룹 하나, 셸 탭 하나. */
        fun initial(): TerminalWorkspace =
            TerminalWorkspace(panels = emptyList(), selectedPanelId = null, nextId = 1).addPanel()
    }
}

private fun PaneNode.replaceGroup(groupId: Long, transform: (PaneNode.Group) -> PaneNode): PaneNode =
    when (this) {
        is PaneNode.Group -> if (id == groupId) transform(this) else this
        is PaneNode.Split -> copy(
            first = first.replaceGroup(groupId, transform),
            second = second.replaceGroup(groupId, transform),
        )
    }

private class Removal(val root: PaneNode, val neighbor: Long)

private fun PaneNode.isGroup(groupId: Long): Boolean = this is PaneNode.Group && id == groupId

private val PaneNode.groupIds: List<Long>
    get() = groups.map { it.id }

// 첫째 자식이 빠지면 둘째의 맨 앞 그룹이, 둘째가 빠지면 첫째의 맨 뒤 그룹이 닫힌 자리와 맞닿아 있다.
private fun PaneNode.remove(groupId: Long): Removal? =
    when (this) {
        is PaneNode.Group -> null

        is PaneNode.Split -> when {
            first.isGroup(groupId) -> Removal(second, second.groupIds.first())
            second.isGroup(groupId) -> Removal(first, first.groupIds.last())
            groupId in first.groupIds -> first.remove(groupId)?.let { Removal(copy(first = it.root), it.neighbor) }
            else -> second.remove(groupId)?.let { Removal(copy(second = it.root), it.neighbor) }
        }
    }

private fun PaneNode.findSplit(splitId: Long): PaneNode.Split? =
    when (this) {
        is PaneNode.Group -> null
        is PaneNode.Split -> if (id == splitId) this else first.findSplit(splitId) ?: second.findSplit(splitId)
    }

private fun PaneNode.withRatio(splitId: Long, ratio: Float): PaneNode =
    when (this) {
        is PaneNode.Group -> this
        is PaneNode.Split -> if (id == splitId) {
            copy(ratio = ratio)
        } else {
            copy(first = first.withRatio(splitId, ratio), second = second.withRatio(splitId, ratio))
        }
    }
