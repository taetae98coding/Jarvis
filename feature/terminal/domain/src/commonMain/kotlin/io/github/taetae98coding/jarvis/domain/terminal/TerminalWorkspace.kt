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
 * [claudeSessionId] 는 [TerminalProgram.Claude] 탭에만, [url] 은 [TerminalProgram.Browser] 탭에만,
 * [deviceId]·[deviceName]·[devicePlatform] 은 [TerminalProgram.Device] 탭에만, [filePath]·[commitHash] 는 [TerminalProgram.File] 탭에만 있다. [deviceName]·[devicePlatform] 은
 * 고를 때의 값이고, [devicePlatform] 이 null 이면 이 값을 저장하기 전에 만든 탭이다.
 * [commitHash] 가 있으면 디스크가 아니라 그 커밋 시점의 파일을 보이는 커밋 파일 탭이다(docs/common/terminal-commit-file.html).
 * [command] 가 있는 셸 탭은 그 명령으로 시작하고 끝나면 셸로 남는 실행 탭이다. [commandTyped] 가 거짓이면 로그인 셸 대신
 * 명령을 돌리고(앱 실행 스크립트), 참이면 로그인 셸을 띄운 뒤 명령을 프롬프트에 쳐 넣는다(사용자 명령). [commandTitle] 이
 * 자동 제목이다(docs/common/terminal-run.html R9·R16).
 *
 * [claudeCheckedAt] 은 사용자가 본 마지막 끝난 결과의 [ClaudeActivity.Finished.at] 이다(docs/common/terminal-claude-status.html).
 *
 * [name] 은 사용자가 정한 이름이다. null 이면 화면이 창의 제목이나 순번으로 자동 제목을 짓는다.
 */
data class TerminalTab(
    val id: Long,
    val program: TerminalProgram = TerminalProgram.Shell,
    val directory: String? = null,
    val claudeSessionId: String? = null,
    val url: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val devicePlatform: DevicePlatform? = null,
    val name: String? = null,
    val claudeCheckedAt: Long? = null,
    val filePath: String? = null,
    val commitHash: String? = null,
    val command: String? = null,
    val commandTitle: String? = null,
    val commandTyped: Boolean = false,
) {
    /** 커밋 파일 탭의 제목·요약에 쓰는 해시 앞 7자리. */
    val shortCommitHash: String?
        get() = commitHash?.take(ShortHashLength)

    val kind: TerminalTabKind
        get() = when (program) {
            TerminalProgram.Shell -> if (command != null) TerminalTabKind.Run else TerminalTabKind.Terminal
            TerminalProgram.Claude -> TerminalTabKind.Claude
            TerminalProgram.Browser -> TerminalTabKind.Browser
            TerminalProgram.File -> TerminalTabKind.File
            TerminalProgram.Device -> when (devicePlatform) {
                DevicePlatform.Android -> TerminalTabKind.Android
                DevicePlatform.IOS -> TerminalTabKind.IOS
                null -> TerminalTabKind.Device
            }
        }

    companion object {
        const val DefaultBrowserUrl = "https://www.google.com"

        const val ShortHashLength = 7
    }
}

/** 탭 하나가 어느 패널·그룹에 있는지. */
data class TabLocation(
    val panel: TerminalPanel,
    val group: PaneNode.Group,
    val tab: TerminalTab,
)

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
 * [parentId] 가 있으면 그 패널 아래 들여쓰는 워크트리 패널이다. 부모는 늘 최상위 패널이라 한 단계뿐이다.
 * [branch]·[baseBranch] 는 워크트리 패널이 만들 때 기억한 브랜치와 기준 브랜치다. 이름을 바꿔도 그대로다.
 * [commands]·[androidRun]·[iosRun] 은 최상위 패널에만 의미가 있다. 워크트리 패널은 부모의 것을 쓴다
 * (docs/common/terminal-run.html R14·R17).
 */
data class TerminalPanel(
    val id: Long,
    val name: String,
    val root: PaneNode?,
    val focusedGroupId: Long?,
    val directory: String? = null,
    val parentId: Long? = null,
    val branch: String? = null,
    val baseBranch: String? = null,
    val commands: List<TerminalCommand> = emptyList(),
    val androidRun: AndroidRunChoice? = null,
    val iosRun: IosRunChoice? = null,
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

    /** 워크트리 패널이 아닌 패널. 목록은 이것마다 [children] 을 아래에 들여써 그린다. */
    val topLevelPanels: List<TerminalPanel>
        get() = panels.filter { it.parentId == null }

    /** [panelId] 아래의 워크트리 패널. 목록 순서다. */
    fun children(panelId: Long): List<TerminalPanel> = panels.filter { it.parentId == panelId }

    /**
     * 패널을 끝에 붙이고 고른다. [name]·[directory] 는 앞뒤 공백을 떼고, 비면 [nextPanelName]·폴더 없음이다.
     * [claudeSessionId] 가 있으면 패널 폴더에서 그 세션의 Claude 탭 하나로 시작하고, 없으면 그룹이 없는 빈 패널이다.
     */
    fun addPanel(name: String? = null, directory: String? = null, claudeSessionId: String? = null): TerminalWorkspace {
        val panel = newPanel(name, directory)

        return copy(panels = panels + panel, selectedPanelId = panel.id, nextId = nextId + 1).startClaude(claudeSessionId)
    }

    /**
     * [addPanel] 과 같은 패널을 [parentId] 패널의 워크트리 패널로 붙이고 고른다. 부모가 워크트리 패널이면 그
     * 부모의 부모 아래 형제로 들어간다(한 단계). 부모와 그 워크트리 패널들 바로 뒤에 놓인다. 모르는 부모면 그대로다.
     * [branch]·[baseBranch] 는 앞뒤 공백을 떼고 비면 없는 것이다.
     */
    fun addWorktreePanel(
        parentId: Long,
        name: String? = null,
        directory: String? = null,
        branch: String? = null,
        baseBranch: String? = null,
        claudeSessionId: String? = null,
    ): TerminalWorkspace {
        val parent = findPanel { it.id == parentId } ?: return this
        val rootId = parent.parentId ?: parent.id
        val familyEnd = panels.indexOfLast { it.id == rootId || it.parentId == rootId }
        val panel = newPanel(name, directory).copy(
            parentId = rootId,
            branch = branch?.trim()?.ifEmpty { null },
            baseBranch = baseBranch?.trim()?.ifEmpty { null },
        )
        val inserted = panels.toMutableList().apply { add(familyEnd + 1, panel) }

        return copy(panels = inserted, selectedPanelId = panel.id, nextId = nextId + 1).startClaude(claudeSessionId)
    }

    // 막 붙여 고른 빈 패널에 첫 탭을 연다. 탭 디렉터리는 startDirectory 가 패널 폴더로 채운다.
    private fun startClaude(claudeSessionId: String?): TerminalWorkspace =
        if (claudeSessionId == null) this else addTab(program = TerminalProgram.Claude, directory = startDirectory(), claudeSessionId = claudeSessionId)

    // 패널 id 로 nextId 를 쓴다. 부르는 쪽이 nextId 를 1 늘린다.
    private fun newPanel(name: String?, directory: String?): TerminalPanel =
        TerminalPanel(
            id = nextId,
            name = name?.trim()?.ifEmpty { null } ?: nextPanelName,
            root = null,
            focusedGroupId = null,
            directory = directory?.trim()?.ifEmpty { null },
        )

    /** 앞뒤 공백을 뗀다. 비어 있으면 바꾸지 않는다. */
    fun renamePanel(panelId: Long, name: String): TerminalWorkspace {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this

        return replacePanel(panelId) { it.copy(name = trimmed) }
    }

    /**
     * 패널과 그 아래 워크트리 패널을 함께 닫는다. 남는 패널이 없게 되는 닫기는 하지 않는다. 선택된 패널이
     * 닫히면 닫힌 것들 뒤에 오는 첫 패널(없으면 앞 패널)이 선택된다.
     */
    fun closePanel(panelId: Long): TerminalWorkspace {
        val index = panels.indexOfFirst { it.id == panelId }
        if (index < 0) return this

        val closing = children(panelId).map { it.id }.toSet() + panelId
        val remaining = panels.filter { it.id !in closing }
        if (remaining.isEmpty()) return this

        val selected = if (selectedPanelId !in closing) {
            selectedPanelId
        } else {
            panels.drop(index).firstOrNull { it.id !in closing }?.id ?: remaining.last().id
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
        deviceId: String? = null,
        deviceName: String? = null,
        devicePlatform: DevicePlatform? = null,
        filePath: String? = null,
        commitHash: String? = null,
        command: String? = null,
        commandTitle: String? = null,
        commandTyped: Boolean = false,
    ): TerminalWorkspace {
        val panel = (if (groupId == null) selectedPanel else findPanel { panel -> panel.groups.any { it.id == groupId } })
            ?: return this
        val group = if (groupId == null) panel.focusedGroup else panel.groups.first { it.id == groupId }
        val tabId = nextId
        val tab = TerminalTab(
            tabId, program, directory, claudeSessionId, url, deviceId, deviceName, devicePlatform,
            filePath = filePath, commitHash = commitHash, command = command, commandTitle = commandTitle, commandTyped = commandTyped,
        )

        if (group == null) {
            val newGroupId = nextId + 1

            return replacePanel(panel.id) { it.copy(root = PaneNode.Group(newGroupId, listOf(tab), tabId), focusedGroupId = newGroupId) }
                .copy(nextId = nextId + 2)
        }

        return replaceGroup(group.id) { it.copy(tabs = it.tabs + tab, selectedTabId = tabId) }
            .focusGroup(group.id)
            .copy(nextId = nextId + 1)
    }

    /**
     * 선택된 패널에 [path] 의 파일 탭이 있으면 그 탭을 고르고, 없으면 포커스된 그룹(없으면 새 그룹)에 파일 탭을 열어 고른다.
     * 다른 패널의 같은 파일 탭은 보지 않는다(docs/common/terminal-side-bar.html#implementation).
     */
    fun openFile(path: String): TerminalWorkspace {
        val opened = selectedPanel?.tabs?.firstOrNull { it.program == TerminalProgram.File && it.filePath == path && it.commitHash == null }

        return if (opened != null) selectTab(opened.id) else addTab(program = TerminalProgram.File, filePath = path)
    }

    /**
     * [openFile] 과 같되 커밋 [hash] 시점의 [path] 를 보이는 커밋 파일 탭이다. 같은 경로·같은 해시의 탭만 다시 고르고, 같은 경로의
     * 보통 파일 탭이나 다른 해시의 탭은 따로 연다(docs/common/terminal-commit-file.html K1).
     */
    fun openCommitFile(path: String, hash: String): TerminalWorkspace {
        val opened = selectedPanel?.tabs?.firstOrNull { it.program == TerminalProgram.File && it.filePath == path && it.commitHash == hash }

        return if (opened != null) selectTab(opened.id) else addTab(program = TerminalProgram.File, filePath = path, commitHash = hash)
    }

    /** 사이드 바가 보이는 폴더. 선택된 패널의 폴더, 없으면 포커스된 탭의 작업 디렉터리다. */
    val sideBarDirectory: String?
        get() = selectedPanel?.directory ?: focusedTab?.directory

    /**
     * [groupId] 그룹의 탭 줄 끝에 탭을 붙이되 고르지 않는다. 선택된 패널, 포커스된 그룹, 그룹마다 선택된 탭이
     * 그대로다 — Claude 가 연 탭이 사용자가 보던 창을 가리지 않게 한다(docs/common/mcp-server.html R5). 붙는 탭의
     * id 는 부르기 전의 [nextId] 다. 모르는 그룹이면 그대로다.
     */
    fun appendTab(
        groupId: Long,
        program: TerminalProgram,
        url: String? = null,
        deviceId: String? = null,
        deviceName: String? = null,
        devicePlatform: DevicePlatform? = null,
    ): TerminalWorkspace {
        if (findGroup { it.id == groupId } == null) return this

        val tab = TerminalTab(nextId, program, url = url, deviceId = deviceId, deviceName = deviceName, devicePlatform = devicePlatform)

        return replaceGroup(groupId) { it.copy(tabs = it.tabs + tab) }.copy(nextId = nextId + 1)
    }

    /** [sessionId] 를 가진 Claude 탭과 그 탭이 있는 패널·그룹. 선택되지 않은 패널도 찾는다. */
    fun findClaudeTab(sessionId: String): TabLocation? =
        panels.firstNotNullOfOrNull { panel ->
            panel.groups.firstNotNullOfOrNull { group ->
                group.tabs.firstOrNull { it.program == TerminalProgram.Claude && it.claudeSessionId == sessionId }
                    ?.let { TabLocation(panel, group, it) }
            }
        }

    /** 포커스된 그룹을 나눠 셸 탭 하나짜리 새 그룹을 오른쪽·아래에 두고 포커스한다. Claude·브라우저·기기는 새 탭 메뉴로만 뜬다. */
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

    /** [panelId] 의 명령·실행 선택을 가진 패널. 워크트리 패널이면 부모다. */
    fun runOwner(panelId: Long): TerminalPanel? {
        val panel = findPanel { it.id == panelId } ?: return null

        return panel.parentId?.let { parentId -> findPanel { it.id == parentId } } ?: panel
    }

    fun commandsOf(panelId: Long): List<TerminalCommand> = runOwner(panelId)?.commands.orEmpty()

    /** 앞뒤 공백을 떼고 목록 끝에 붙인다. 명령이 비면 그대로다. 이름이 비면 null 이다. */
    fun addCommand(panelId: Long, title: String?, command: String): TerminalWorkspace {
        val owner = runOwner(panelId) ?: return this
        val trimmed = command.trim().ifEmpty { return this }
        val added = TerminalCommand(nextId, title?.trim()?.ifEmpty { null }, trimmed)

        return replacePanel(owner.id) { it.copy(commands = it.commands + added) }.copy(nextId = nextId + 1)
    }

    /** [addCommand] 와 같은 규칙으로 고친다. 명령이 비거나 모르는 id 면 그대로다. */
    fun editCommand(panelId: Long, commandId: Long, title: String?, command: String): TerminalWorkspace {
        val owner = runOwner(panelId) ?: return this
        val trimmed = command.trim().ifEmpty { return this }
        if (owner.commands.none { it.id == commandId }) return this

        return replacePanel(owner.id) { panel ->
            panel.copy(commands = panel.commands.map { if (it.id == commandId) TerminalCommand(commandId, title?.trim()?.ifEmpty { null }, trimmed) else it })
        }
    }

    fun removeCommand(panelId: Long, commandId: Long): TerminalWorkspace {
        val owner = runOwner(panelId) ?: return this

        return replacePanel(owner.id) { panel -> panel.copy(commands = panel.commands.filterNot { it.id == commandId }) }
    }

    fun rememberAndroidRun(panelId: Long, choice: AndroidRunChoice): TerminalWorkspace {
        val owner = runOwner(panelId) ?: return this

        return replacePanel(owner.id) { it.copy(androidRun = choice) }
    }

    fun rememberIosRun(panelId: Long, choice: IosRunChoice): TerminalWorkspace {
        val owner = runOwner(panelId) ?: return this

        return replacePanel(owner.id) { it.copy(iosRun = choice) }
    }

    /**
     * [groupId] 그룹(null 이면 포커스된 그룹, 없으면 새 그룹)의 끝에 [command] 로 시작하는 실행 탭을 붙여 고르고 포커스한다.
     * [typed] 면 셸을 띄운 뒤 명령을 쳐 넣는 사용자 명령 탭이다(R16).
     * [mirror] 가 있으면 그 패널에 같은 기기의 기기 탭이 있을 때 그 탭을 그 그룹에서 고르고, 없으면 실행 탭의 그룹을 좌우로
     * 나눠 오른쪽에 기기 탭 하나짜리 그룹을 둔다. 포커스는 실행 탭의 그룹에 남는다(docs/common/terminal-run.html R9·R10).
     */
    fun runInGroup(
        groupId: Long?,
        directory: String?,
        command: String,
        title: String,
        mirror: RunMirror? = null,
        typed: Boolean = false,
    ): TerminalWorkspace {
        val runTabId = nextId
        val added = addTab(groupId, directory = directory, command = command, commandTitle = title, commandTyped = typed)
        if (mirror == null || added === this) return added

        val panel = added.findPanel { panel -> panel.tabs.any { it.id == runTabId } } ?: return added
        val runGroup = panel.groups.first { group -> group.tabs.any { it.id == runTabId } }
        val existing = panel.groups.firstNotNullOfOrNull { group ->
            group.tabs.firstOrNull { it.program == TerminalProgram.Device && it.deviceId == mirror.deviceId }?.let { group to it }
        }
        if (existing != null) {
            val (group, tab) = existing
            return added.replaceGroup(group.id) { it.copy(selectedTabId = tab.id) }
        }

        val splitId = added.nextId
        val deviceGroupId = added.nextId + 1
        val deviceTabId = added.nextId + 2
        val deviceTab = TerminalTab(
            id = deviceTabId,
            program = TerminalProgram.Device,
            deviceId = mirror.deviceId,
            deviceName = mirror.deviceName,
            devicePlatform = mirror.platform,
        )

        return added
            .replaceGroup(runGroup.id) { group ->
                PaneNode.Split(splitId, SplitDirection.SideBySide, first = group, second = PaneNode.Group(deviceGroupId, listOf(deviceTab), deviceTabId), ratio = RunRatio)
            }
            .copy(nextId = added.nextId + 3)
    }

    /**
     * 모든 실행·명령 탭을 같은 폴더의 셸 탭으로 만든다. 앱을 켤 때 한 번 불러 되살아난 탭이 명령을 다시 돌리지 않게 한다
     * (docs/common/terminal-run.html R18). 지울 것이 없으면 자신이다.
     */
    fun withoutCommands(): TerminalWorkspace {
        if (panels.none { panel -> panel.tabs.any { it.command != null } }) return this

        return copy(
            panels = panels.map { panel ->
                panel.copy(root = panel.root?.mapTabs { it.copy(command = null, commandTitle = null, commandTyped = false) })
            },
        )
    }

    fun setDirectory(tabId: Long, directory: String): TerminalWorkspace = replaceTab(tabId) { it.copy(directory = directory) }

    fun setUrl(tabId: Long, url: String): TerminalWorkspace = replaceTab(tabId) { it.copy(url = url) }

    /** 앞뒤 공백을 뗀다. 비어 있으면 사용자가 정한 이름을 지워 자동 제목으로 돌아간다. */
    fun renameTab(tabId: Long, name: String): TerminalWorkspace = replaceTab(tabId) { it.copy(name = name.trim().ifEmpty { null }) }

    /**
     * 지금 보이는 Claude 탭 중 끝난 결과가 있는 탭의 확인 기록을 그 결과로 올린다. 올릴 것이 없으면 자신이다.
     * 보이는지는 [visibleTabs] 이고, 앱 창이 포커스를 가졌는지는 부르는 쪽이 가린다.
     */
    fun checkVisibleClaudeTabs(activities: Map<String, ClaudeActivity>): TerminalWorkspace =
        visibleTabs.fold(this) { workspace, tab ->
            val finished = tab.claudeSessionId?.let(activities::get) as? ClaudeActivity.Finished
            if (finished == null || (tab.claudeCheckedAt ?: Long.MIN_VALUE) >= finished.at) {
                workspace
            } else {
                workspace.replaceTab(tab.id) { it.copy(claudeCheckedAt = finished.at) }
            }
        }

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

        /** 실행 탭 그룹이 기기 탭 그룹과 나눌 때 차지하는 몫(docs/common/terminal-run.html R10). */
        const val RunRatio = 0.6f

        /**
         * 처음 켰을 때. 패널 하나, 그룹 하나, 셸 탭 하나. 창으로 만드는 패널과 달리 Claude 가 아니다 — data 계층이
         * 저장 전까지 읽을 때마다 다시 부르므로, 무작위 sessionId 가 매번 달라진다(docs/common/terminal-panel-create.html).
         */
        fun initial(): TerminalWorkspace =
            TerminalWorkspace(panels = emptyList(), selectedPanelId = null, nextId = 1).addPanel().addTab()
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

private fun PaneNode.mapTabs(transform: (TerminalTab) -> TerminalTab): PaneNode =
    when (this) {
        is PaneNode.Group -> copy(tabs = tabs.map(transform))
        is PaneNode.Split -> copy(first = first.mapTabs(transform), second = second.mapTabs(transform))
    }

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
