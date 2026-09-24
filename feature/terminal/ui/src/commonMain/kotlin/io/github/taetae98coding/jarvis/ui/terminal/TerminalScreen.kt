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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.StateFlow

const val TerminalScreenTestTag = "terminal:screen"
const val TerminalNewShellTabTestTag = "terminal:new-tab-menu:shell"
const val TerminalNewClaudeTabTestTag = "terminal:new-tab-menu:claude"
const val TerminalEmptyPanelTestTag = "terminal:empty-panel"

/** 그룹의 새 탭 버튼. null 은 그룹이 없는 빈 패널의 버튼이다. */
fun terminalNewTabTestTag(groupId: Long?): String = "terminal:new-tab:${groupId ?: "none"}"

fun terminalGroupTestTag(id: Long): String = "terminal:group:$id"

fun terminalTabTestTag(id: Long): String = "terminal:tab:$id"

fun terminalTabCloseTestTag(id: Long): String = "terminal:tab-close:$id"

@Composable
internal fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()
    val drag = remember { TerminalTabDragState() }

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
                    canOpenClaude = viewModel.isClaudeSupported,
                    onSelect = viewModel::selectPanel,
                    onRename = viewModel::renamePanel,
                    onClose = viewModel::closePanel,
                    onAdd = viewModel::addPanel,
                )

                val panel = current.selectedPanel
                val root = panel?.root
                if (panel == null || root == null) {
                    EmptyPanel(viewModel = viewModel, modifier = Modifier.weight(1f).fillMaxHeight())
                    return@Row
                }

                key(panel.id) {
                    PaneTree(
                        node = root,
                        panel = panel,
                        viewModel = viewModel,
                        drag = drag,
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
private fun EmptyPanel(viewModel: TerminalViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            NewTabButton(
                groupId = null,
                onNewTab = { viewModel.addTab() },
                onNewClaudeTab = { viewModel.addClaudeTab() }.takeIf { viewModel.isClaudeSupported },
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
                modifier = modifier,
            )
        }

        is PaneNode.Split -> SplitPane(node, panel, viewModel, drag, modifier)
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
                titleOf = { tabId -> viewModel.pane(tabId)?.title },
                onSelect = viewModel::selectTab,
                onClose = viewModel::closeTab,
                onDock = { tabId, target -> viewModel.dockTab(tabId, target.groupId, target.edge) },
                onNewTab = { viewModel.addTab(group.id) },
                onNewClaudeTab = { viewModel.addClaudeTab(group.id) }.takeIf { viewModel.isClaudeSupported },
            )

            val tab = group.selectedTab
            val pane = viewModel.pane(tab.id)
            if (pane != null) {
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
    titleOf: (Long) -> StateFlow<String?>?,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onDock: (Long, TerminalDropTarget) -> Unit,
    onNewTab: () -> Unit,
    onNewClaudeTab: (() -> Unit)?,
) {
    Row(
        // 새 탭 버튼이 탭 높이를 따라가도록 줄 높이를 탭에 맞춘다.
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        group.tabs.forEachIndexed { index, tab ->
            val title = tabTitle(titleOf(tab.id), index, tab)

            TerminalTab(
                title = title,
                selected = tab.id == group.selectedTabId,
                onSelect = { onSelect(tab.id) },
                onClose = { onClose(tab.id) },
                modifier = Modifier
                    .testTag(terminalTabTestTag(tab.id))
                    .terminalTabDragSource(drag, tab.id, title) { target -> onDock(tab.id, target) },
                closeModifier = Modifier.testTag(terminalTabCloseTestTag(tab.id)),
            )
        }

        NewTabButton(groupId = group.id, onNewTab = onNewTab, onNewClaudeTab = onNewClaudeTab)
    }
}

// 고를 것이 셸 하나뿐이면 메뉴를 띄우지 않는다. 항목 하나짜리 메뉴는 한 번 더 누르게 할 뿐이다.
@Composable
private fun NewTabButton(
    groupId: Long?,
    onNewTab: () -> Unit,
    onNewClaudeTab: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TerminalNewTabButton(
            onClick = { if (onNewClaudeTab == null) onNewTab() else expanded = true },
            modifier = Modifier.testTag(terminalNewTabTestTag(groupId)),
        )

        if (onNewClaudeTab != null) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("터미널") },
                    leadingIcon = { Icon(imageVector = JarvisIcons.Terminal, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onNewTab()
                    },
                    modifier = Modifier.testTag(TerminalNewShellTabTestTag),
                )
                DropdownMenuItem(
                    text = { Text("Claude (YOLO)") },
                    leadingIcon = { Icon(imageVector = JarvisIcons.Claude, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onNewClaudeTab()
                    },
                    modifier = Modifier.testTag(TerminalNewClaudeTabTestTag),
                )
            }
        }
    }
}

@Composable
private fun tabTitle(source: StateFlow<String?>?, index: Int, tab: TerminalTab): String {
    val title = source?.collectAsStateWithLifecycle()?.value
    val fallback = when (tab.program) {
        TerminalProgram.Shell -> "셸"
        TerminalProgram.Claude -> "Claude"
    }

    return title?.takeIf { it.isNotBlank() } ?: "$fallback ${index + 1}"
}

@Composable
private fun SplitPane(
    node: PaneNode.Split,
    panel: TerminalPanel,
    viewModel: TerminalViewModel,
    drag: TerminalTabDragState,
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
            PaneTree(node.first, panel, viewModel, drag, Modifier.weight(ratio).fillMaxHeight())
            divider()
            PaneTree(node.second, panel, viewModel, drag, Modifier.weight(1f - ratio).fillMaxHeight())
        }
    } else {
        Column(modifier = modifier.onSizeChanged { size = it }) {
            PaneTree(node.first, panel, viewModel, drag, Modifier.weight(ratio).fillMaxWidth())
            divider()
            PaneTree(node.second, panel, viewModel, drag, Modifier.weight(1f - ratio).fillMaxWidth())
        }
    }
}
