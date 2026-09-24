package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalPanel
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import io.github.taetae98coding.jarvis.ui.device.DeviceScreens
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.currentKoinScope

const val TerminalScreenTestTag = "terminal:screen"
const val TerminalNewShellTabTestTag = "terminal:new-tab-menu:shell"
const val TerminalNewClaudeTabTestTag = "terminal:new-tab-menu:claude"
const val TerminalNewBrowserTabTestTag = "terminal:new-tab-menu:browser"
const val TerminalNewDeviceTabEmptyTestTag = "terminal:new-tab-menu:devices-empty"
const val TerminalEmptyPanelTestTag = "terminal:empty-panel"
const val TerminalTabNameFieldTestTag = "terminal:tab-name-field"

/** 그룹의 새 탭 버튼. null 은 그룹이 없는 빈 패널의 버튼이다. */
fun terminalNewTabTestTag(groupId: Long?): String = "terminal:new-tab:${groupId ?: "none"}"

fun terminalNewDeviceTabTestTag(deviceId: String): String = "terminal:new-tab-menu:device:$deviceId"

fun terminalGroupTestTag(id: Long): String = "terminal:group:$id"

fun terminalTabTestTag(id: Long): String = "terminal:tab:$id"

fun terminalTabCloseTestTag(id: Long): String = "terminal:tab-close:$id"

fun terminalTabKindTestTag(id: Long): String = "terminal:tab-kind:$id"

fun terminalTabTitleTestTag(id: Long): String = "terminal:tab-title:$id"

@Composable
internal fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()
    val worktrees by viewModel.worktrees.collectAsStateWithLifecycle()
    val drag = remember { TerminalTabDragState() }
    // 기기 기능이 빠진 조립에서는 없다. 그때는 메뉴에 기기 구획이 없다. getKoin() 은 처음 본 Koin 을 붙잡아 두어
    // Koin 을 다시 세우면(테스트) 닫힌 것을 돌려주므로, 닫히면 다시 찾는 currentKoinScope() 로 받는다.
    val scope = currentKoinScope()
    val devices = remember(scope) { scope.getOrNull<DeviceScreens>() }

    // 창에 포커스가 있을 때 선택된 패널에 보이는 Claude 탭만 "보고 있음" 이다(docs/common/claude-notification.html R4).
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val watched = workspace?.takeIf { windowFocused }?.visibleTabs?.mapNotNullTo(mutableSetOf()) { it.claudeSessionId }.orEmpty()
    DisposableEffect(viewModel, watched) {
        viewModel.watchClaude(watched)
        onDispose { viewModel.watchClaude(emptySet()) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(TerminalScreenTestTag)
            // 루트에서 먼저 가로채야 포커스된 창의 입력 필드보다 앞선다.
            .onPreviewKeyEvent { event -> onShortcut(event, viewModel) }
            .onGloballyPositioned { drag.screen = it },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        ) {
            JarvisTopBar(title = "터미널", onBack = onBack)

            val current = workspace ?: return@Column

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
            ) {
                TerminalPanelList(
                    panels = current.panels,
                    selectedPanelId = current.selectedPanelId,
                    nextPanelName = current.nextPanelName,
                    worktrees = worktrees,
                    onSelect = viewModel::selectPanel,
                    onRename = viewModel::renamePanel,
                    onClose = viewModel::closePanel,
                    onAdd = viewModel::addPanel,
                    onAddWorktree = viewModel::addWorktreePanel,
                    onCloseWorktree = viewModel::closeWorktreePanel,
                )

                val panel = current.selectedPanel
                val root = panel?.root
                if (panel == null || root == null) {
                    EmptyPanel(viewModel = viewModel, devices = devices, modifier = Modifier.weight(1f).fillMaxHeight())
                    return@Row
                }

                key(panel.id) {
                    PaneTree(
                        node = root,
                        panel = panel,
                        viewModel = viewModel,
                        drag = drag,
                        devices = devices,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }

        TerminalDragGhost(drag)
    }
}

/** 그룹이 없는 패널. + 하나가 그룹을 만들어 탭을 넣는다. */
@Composable
private fun EmptyPanel(viewModel: TerminalViewModel, devices: DeviceScreens?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            NewTabButton(
                groupId = null,
                onNewTab = { viewModel.addTab() },
                onNewClaudeTab = { viewModel.addClaudeTab() }.takeIf { viewModel.isClaudeSupported },
                onNewBrowserTab = { viewModel.addBrowserTab() }.takeIf { viewModel.isBrowserSupported },
                devices = devices,
                onNewDeviceTab = { viewModel.addDeviceTab(null, it) },
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).testTag(TerminalEmptyPanelTestTag), contentAlignment = Alignment.Center) {
            Text(
                text = "탭이 없습니다. 새 탭(+)으로 터미널이나 Claude 를 엽니다.",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun onShortcut(event: KeyEvent, viewModel: TerminalViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown || !event.isMetaPressed) return false

    when (event.key) {
        Key.D -> if (event.isShiftPressed) viewModel.splitStacked() else viewModel.splitSideBySide()
        Key.T -> viewModel.addTab()
        Key.W -> viewModel.closeFocusedTab()
        Key.RightBracket -> if (event.isShiftPressed) viewModel.selectAdjacentTab(1) else viewModel.focusAdjacentGroup(1)
        Key.LeftBracket -> if (event.isShiftPressed) viewModel.selectAdjacentTab(-1) else viewModel.focusAdjacentGroup(-1)
        else -> {
            val index = TabNumberKeys.indexOf(event.key)
            if (index < 0) return false

            viewModel.selectTabAt(index)
        }
    }

    return true
}

private val TabNumberKeys = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

@Composable
private fun PaneTree(
    node: PaneNode,
    panel: TerminalPanel,
    viewModel: TerminalViewModel,
    drag: TerminalTabDragState,
    devices: DeviceScreens?,
    modifier: Modifier = Modifier,
) {
    when (node) {
        is PaneNode.Group -> key(node.id) {
            TerminalGroup(
                group = node,
                focused = node.id == panel.focusedGroup?.id,
                showFocusBorder = panel.root is PaneNode.Split,
                viewModel = viewModel,
                drag = drag,
                devices = devices,
                modifier = modifier,
            )
        }

        is PaneNode.Split -> SplitPane(node, panel, viewModel, drag, devices, modifier)
    }
}

/** 나뉜 칸 하나: 위에 자기 탭 줄, 아래에 선택된 탭의 창. 끌어 놓기의 대상이기도 하다. */
@Composable
private fun TerminalGroup(
    group: PaneNode.Group,
    focused: Boolean,
    showFocusBorder: Boolean,
    viewModel: TerminalViewModel,
    drag: TerminalTabDragState,
    devices: DeviceScreens?,
    modifier: Modifier = Modifier,
) {
    val tabIds = group.tabs.map { it.id }

    Box(
        modifier = modifier
            .testTag(terminalGroupTestTag(group.id))
            .onGloballyPositioned { drag.registerGroup(group.id, tabIds, it) },
    ) {
        DisposableEffect(drag, group.id) {
            onDispose { drag.unregisterGroup(group.id) }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
        ) {
            TerminalTabRow(
                group = group,
                drag = drag,
                titleOf = viewModel::title,
                onSelect = viewModel::selectTab,
                onClose = viewModel::closeTab,
                onRename = viewModel::renameTab,
                onDock = { tabId, target -> viewModel.dockTab(tabId, target.groupId, target.edge) },
                onNewTab = { viewModel.addTab(group.id) },
                onNewClaudeTab = { viewModel.addClaudeTab(group.id) }.takeIf { viewModel.isClaudeSupported },
                onNewBrowserTab = { viewModel.addBrowserTab(group.id) }.takeIf { viewModel.isBrowserSupported },
                devices = devices,
                onNewDeviceTab = { viewModel.addDeviceTab(group.id, it) },
                onMenuExpandedChange = { drag.menuOpen = it },
            )

            val tab = group.selectedTab
            val pane = viewModel.pane(tab.id)
            if (tab.program == TerminalProgram.Device) {
                key(tab.id) {
                    TerminalDevice(
                        tab = tab,
                        devices = devices,
                        onFocus = { viewModel.focusGroup(group.id) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            } else if (tab.program == TerminalProgram.Browser) {
                key(tab.id) {
                    TerminalBrowser(
                        tab = tab,
                        isSupported = viewModel.isBrowserSupported,
                        isChromeImportSupported = viewModel.isChromeImportSupported,
                        pageHidden = drag.coversPages,
                        onUrl = { viewModel.setUrl(tab.id, it) },
                        onTitle = { viewModel.setBrowserTitle(tab.id, it) },
                        onFocus = { viewModel.focusGroup(group.id) },
                        chromeProfiles = { viewModel.chromeProfiles() },
                        importCookies = { viewModel.importCookies(it) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            } else if (pane != null) {
                key(tab.id) {
                    TerminalPane(
                        state = pane,
                        focused = focused,
                        showFocusBorder = showFocusBorder,
                        onFocus = { viewModel.focusGroup(group.id) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }

        val target = drag.target()
        if (target?.groupId == group.id) {
            TerminalDropPreview(groupId = group.id, edge = target.edge)
        }
    }
}

@Composable
private fun TerminalTabRow(
    group: PaneNode.Group,
    drag: TerminalTabDragState,
    titleOf: (TerminalTab) -> StateFlow<String?>?,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDock: (Long, TerminalDropTarget) -> Unit,
    onNewTab: () -> Unit,
    onNewClaudeTab: (() -> Unit)?,
    onNewBrowserTab: (() -> Unit)?,
    devices: DeviceScreens?,
    onNewDeviceTab: (DeviceChoice) -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
) {
    var editingTabId by remember { mutableStateOf<Long?>(null) }

    Row(
        // 새 탭 버튼이 탭 높이를 따라가도록 줄 높이를 탭에 맞춘다.
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        group.tabs.forEachIndexed { index, tab ->
            val title = tabTitle(titleOf(tab), index, tab)
            val editing = editingTabId == tab.id

            TerminalTab(
                title = title,
                kind = tab.kind,
                selected = tab.id == group.selectedTabId,
                editing = editing,
                onSelect = { onSelect(tab.id) },
                onStartRename = { editingTabId = tab.id },
                onRename = { value ->
                    editingTabId = null
                    // 고치지 않고 확정하면 자동 제목이 이름으로 굳지 않게 그대로 둔다.
                    if (value != title) onRename(tab.id, value)
                },
                onCancelRename = { editingTabId = null },
                onClose = { onClose(tab.id) },
                modifier = Modifier
                    .testTag(terminalTabTestTag(tab.id))
                    // 입력칸에서 끌어 글자를 고르는 것이 탭 끌기로 읽히지 않게 편집하는 동안은 떼어 둔다.
                    .then(if (editing) Modifier else Modifier.terminalTabDragSource(drag, tab.id, title) { target -> onDock(tab.id, target) }),
                kindModifier = Modifier.testTag(terminalTabKindTestTag(tab.id)),
                titleModifier = Modifier.testTag(terminalTabTitleTestTag(tab.id)),
                nameFieldModifier = Modifier.testTag(TerminalTabNameFieldTestTag),
                closeModifier = Modifier.testTag(terminalTabCloseTestTag(tab.id)),
            )
        }

        NewTabButton(
            groupId = group.id,
            onNewTab = onNewTab,
            onNewClaudeTab = onNewClaudeTab,
            onNewBrowserTab = onNewBrowserTab,
            devices = devices,
            onNewDeviceTab = onNewDeviceTab,
            onExpandedChange = onMenuExpandedChange,
        )
    }
}

// 고를 것이 셸 하나뿐이면 메뉴를 띄우지 않는다. 항목 하나짜리 메뉴는 한 번 더 누르게 할 뿐이다.
@Composable
private fun NewTabButton(
    groupId: Long?,
    onNewTab: () -> Unit,
    onNewClaudeTab: (() -> Unit)?,
    onNewBrowserTab: (() -> Unit)?,
    devices: DeviceScreens?,
    onNewDeviceTab: (DeviceChoice) -> Unit,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    // 기기 구획은 기기가 없어도 뜬다. 없다는 것도 메뉴가 알려 준다.
    val hasMenu = onNewClaudeTab != null || onNewBrowserTab != null || devices != null

    fun setExpanded(value: Boolean) {
        expanded = value
        onExpandedChange(value)
    }

    // 메뉴가 뜬 채로 버튼이 사라져도(그룹이 닫히는 등) 닫힌 것으로 알린다.
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    DisposableEffect(Unit) { onDispose { if (expanded) currentOnExpandedChange(false) } }

    Box {
        TerminalNewTabButton(
            onClick = { if (hasMenu) setExpanded(true) else onNewTab() },
            modifier = Modifier.testTag(terminalNewTabTestTag(groupId)),
        )

        if (hasMenu) {
            DropdownMenu(expanded = expanded, onDismissRequest = { setExpanded(false) }) {
                DropdownMenuItem(
                    text = { Text("터미널") },
                    leadingIcon = { Icon(imageVector = JarvisIcons.Terminal, contentDescription = null) },
                    onClick = {
                        setExpanded(false)
                        onNewTab()
                    },
                    modifier = Modifier.testTag(TerminalNewShellTabTestTag),
                )
                if (onNewClaudeTab != null) {
                    DropdownMenuItem(
                        text = { Text("Claude (YOLO)") },
                        leadingIcon = { Icon(imageVector = JarvisIcons.Claude, contentDescription = null) },
                        onClick = {
                            setExpanded(false)
                            onNewClaudeTab()
                        },
                        modifier = Modifier.testTag(TerminalNewClaudeTabTestTag),
                    )
                }
                if (onNewBrowserTab != null) {
                    DropdownMenuItem(
                        text = { Text("웹 브라우저") },
                        leadingIcon = { Icon(imageVector = JarvisIcons.Globe, contentDescription = null) },
                        onClick = {
                            setExpanded(false)
                            onNewBrowserTab()
                        },
                        modifier = Modifier.testTag(TerminalNewBrowserTabTestTag),
                    )
                }
                if (devices != null) {
                    DeviceMenuSection(
                        devices = devices,
                        onSelect = { choice ->
                            setExpanded(false)
                            onNewDeviceTab(choice)
                        },
                    )
                }
            }
        }
    }
}

// 메뉴 내용은 메뉴가 떠 있을 때만 컴포즈되므로 기기 목록도 그동안만 센다.
@Composable
private fun DeviceMenuSection(devices: DeviceScreens, onSelect: (DeviceChoice) -> Unit) {
    HorizontalDivider()
    Text(
        text = "기기",
        style = JarvisTheme.typography.labelMedium,
        color = JarvisTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = JarvisTheme.dimens.spacing.m, vertical = JarvisTheme.dimens.spacing.s),
    )

    val choices = devices.choices()
    if (choices.isNullOrEmpty()) {
        DropdownMenuItem(
            text = { Text(if (choices == null) "기기를 찾는 중…" else "화면을 볼 수 있는 기기가 없습니다") },
            onClick = {},
            enabled = false,
            modifier = Modifier.testTag(TerminalNewDeviceTabEmptyTestTag),
        )
        return
    }

    choices.forEach { choice ->
        DropdownMenuItem(
            text = {
                Column {
                    Text(text = choice.name)
                    Text(
                        text = choice.kind,
                        style = JarvisTheme.typography.bodySmall,
                        color = JarvisTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingIcon = { Icon(imageVector = TerminalTabDefaults.kindIcon(choice.tabKind), contentDescription = null) },
            onClick = { onSelect(choice) },
            modifier = Modifier.testTag(terminalNewDeviceTabTestTag(choice.id)),
        )
    }
}

@Composable
private fun tabTitle(source: StateFlow<String?>?, index: Int, tab: TerminalTab): String {
    // 기기 이름은 고를 때 탭에 저장해 둔다. 가려진 탭의 이름을 알려고 목록을 계속 세지 않는다.
    val automatic = if (tab.program == TerminalProgram.Device) tab.deviceName else source?.collectAsStateWithLifecycle()?.value
    val title = tab.name ?: automatic
    val fallback = when (tab.program) {
        TerminalProgram.Shell -> "셸"
        TerminalProgram.Claude -> "Claude"
        TerminalProgram.Browser -> "웹"
        TerminalProgram.Device -> "기기"
    }

    return title?.takeIf { it.isNotBlank() } ?: "$fallback ${index + 1}"
}

@Composable
private fun SplitPane(
    node: PaneNode.Split,
    panel: TerminalPanel,
    viewModel: TerminalViewModel,
    drag: TerminalTabDragState,
    devices: DeviceScreens?,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val sideBySide = node.direction == SplitDirection.SideBySide

    // 끄는 동안의 비율은 화면에만 두고 손을 뗄 때 한 번 저장한다. 프레임마다 저장하면 매번 파일을 쓴다.
    // 저장된 값이 돌아오면 그 값을 따른다.
    var dragRatio by remember(node.id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(node.ratio) { dragRatio = null }
    val ratio = dragRatio ?: node.ratio

    val divider = @Composable {
        val length = if (sideBySide) size.width else size.height
        val dragState = rememberDraggableState { delta ->
            if (length > 0) {
                dragRatio = ((dragRatio ?: node.ratio) + delta / length)
                    .coerceIn(TerminalWorkspace.MinRatio, TerminalWorkspace.MaxRatio)
            }
        }
        val thickness = JarvisTheme.dimens.stroke.thick
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = rememberUpdatedStyleState(interactionSource)

        Box(
            modifier = Modifier
                .then(if (sideBySide) Modifier.width(thickness).fillMaxHeight() else Modifier.height(thickness).fillMaxWidth())
                .hoverable(interactionSource)
                .styleable(styleState, TerminalPaneDefaults.dividerStyle)
                .draggable(
                    state = dragState,
                    orientation = if (sideBySide) Orientation.Horizontal else Orientation.Vertical,
                    interactionSource = interactionSource,
                    onDragStopped = { dragRatio?.let { viewModel.setRatio(node.id, it) } },
                ),
        )
    }

    if (sideBySide) {
        Row(modifier = modifier.onSizeChanged { size = it }) {
            PaneTree(node.first, panel, viewModel, drag, devices, Modifier.weight(ratio).fillMaxHeight())
            divider()
            PaneTree(node.second, panel, viewModel, drag, devices, Modifier.weight(1f - ratio).fillMaxHeight())
        }
    } else {
        Column(modifier = modifier.onSizeChanged { size = it }) {
            PaneTree(node.first, panel, viewModel, drag, devices, Modifier.weight(ratio).fillMaxWidth())
            divider()
            PaneTree(node.second, panel, viewModel, drag, devices, Modifier.weight(1f - ratio).fillMaxWidth())
        }
    }
}
