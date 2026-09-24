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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.leaves
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import kotlinx.coroutines.flow.StateFlow

const val TerminalScreenTestTag = "terminal:screen"
const val TerminalSplitSideTestTag = "terminal:split-side"
const val TerminalSplitStackedTestTag = "terminal:split-stacked"
const val TerminalNewTabTestTag = "terminal:new-tab"
const val TerminalNewShellTabTestTag = "terminal:new-tab:shell"
const val TerminalNewClaudeTabTestTag = "terminal:new-tab:claude"
const val TerminalCloseTestTag = "terminal:close"
const val TerminalEmptyPanelTestTag = "terminal:empty-panel"

fun terminalTabTestTag(id: Long): String = "terminal:tab:$id"

fun terminalTabCloseTestTag(id: Long): String = "terminal:tab-close:$id"

@Composable
internal fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(TerminalScreenTestTag)
            // 루트에서 먼저 가로채야 포커스된 창의 입력 필드보다 앞선다.
            .onPreviewKeyEvent { event -> onShortcut(event, viewModel) },
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
    ) {
        TerminalTopBar(
            onBack = onBack,
            onSplitSideBySide = viewModel::splitSideBySide,
            onSplitStacked = viewModel::splitStacked,
            onNewTab = viewModel::addTab,
            onNewClaudeTab = viewModel::addClaudeTab.takeIf { viewModel.isClaudeSupported },
            onClose = viewModel::closeFocusedPane,
        )

        val current = workspace ?: return@Column

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        ) {
            TerminalPanelList(
                panels = current.panels,
                selectedPanelId = current.selectedPanelId,
                onSelect = viewModel::selectPanel,
                onRename = viewModel::renamePanel,
                onClose = viewModel::closePanel,
                onAdd = viewModel::addPanel,
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
            ) {
                TerminalTabRow(
                    workspace = current,
                    titleOf = { paneId -> viewModel.pane(paneId)?.title },
                    onSelect = viewModel::selectTab,
                    onClose = viewModel::closeTab,
                )

                val tab = current.selectedTab
                if (tab == null) {
                    EmptyPanel(modifier = Modifier.fillMaxWidth().weight(1f))
                    return@Column
                }

                key(tab.id) {
                    PaneTree(
                        node = tab.root,
                        tab = tab,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.testTag(TerminalEmptyPanelTestTag), contentAlignment = Alignment.Center) {
        Text(
            text = "탭이 없습니다. 새 탭(+)으로 터미널이나 Claude 를 엽니다.",
            style = JarvisTheme.typography.bodyMedium,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun onShortcut(event: KeyEvent, viewModel: TerminalViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown || !event.isMetaPressed) return false

    when (event.key) {
        Key.D -> if (event.isShiftPressed) viewModel.splitStacked() else viewModel.splitSideBySide()
        Key.T -> viewModel.addTab()
        Key.W -> viewModel.closeFocusedPane()
        Key.RightBracket -> if (event.isShiftPressed) viewModel.selectAdjacentTab(1) else viewModel.focusAdjacentPane(1)
        Key.LeftBracket -> if (event.isShiftPressed) viewModel.selectAdjacentTab(-1) else viewModel.focusAdjacentPane(-1)
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
private fun TerminalTopBar(
    onBack: () -> Unit,
    onSplitSideBySide: () -> Unit,
    onSplitStacked: () -> Unit,
    onNewTab: () -> Unit,
    onNewClaudeTab: (() -> Unit)?,
    onClose: () -> Unit,
) {
    JarvisTopBar(title = "터미널", onBack = onBack) {
        JarvisIconButton(
            icon = JarvisIcons.SplitSideBySide,
            contentDescription = "좌우 분할",
            onClick = onSplitSideBySide,
            modifier = Modifier.testTag(TerminalSplitSideTestTag),
        )
        JarvisIconButton(
            icon = JarvisIcons.SplitStacked,
            contentDescription = "상하 분할",
            onClick = onSplitStacked,
            modifier = Modifier.testTag(TerminalSplitStackedTestTag),
        )
        NewTabButton(onNewTab = onNewTab, onNewClaudeTab = onNewClaudeTab)
        JarvisIconButton(
            icon = JarvisIcons.Close,
            contentDescription = "닫기",
            onClick = onClose,
            modifier = Modifier.testTag(TerminalCloseTestTag),
        )
    }
}

// 고를 것이 셸 하나뿐이면 메뉴를 띄우지 않는다. 항목 하나짜리 메뉴는 한 번 더 누르게 할 뿐이다.
@Composable
private fun NewTabButton(
    onNewTab: () -> Unit,
    onNewClaudeTab: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        JarvisIconButton(
            icon = JarvisIcons.Add,
            contentDescription = "새 탭",
            onClick = { if (onNewClaudeTab == null) onNewTab() else expanded = true },
            modifier = Modifier.testTag(TerminalNewTabTestTag),
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
private fun TerminalTabRow(
    workspace: TerminalWorkspace,
    titleOf: (Long) -> StateFlow<String?>?,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        workspace.tabs.forEachIndexed { index, tab ->
            val program = tab.root.leaves.firstOrNull { it.paneId == tab.focusedPaneId }?.program ?: TerminalProgram.Shell
            TerminalTab(
                title = tabTitle(titleOf(tab.focusedPaneId), index, program),
                selected = tab.id == workspace.selectedTabId,
                onSelect = { onSelect(tab.id) },
                onClose = { onClose(tab.id) },
                modifier = Modifier.testTag(terminalTabTestTag(tab.id)),
                closeModifier = Modifier.testTag(terminalTabCloseTestTag(tab.id)),
            )
        }
    }
}

@Composable
private fun tabTitle(source: StateFlow<String?>?, index: Int, program: TerminalProgram): String {
    val title = source?.collectAsStateWithLifecycle()?.value
    val fallback = when (program) {
        TerminalProgram.Shell -> "셸"
        TerminalProgram.Claude -> "Claude"
    }

    return title?.takeIf { it.isNotBlank() } ?: "$fallback ${index + 1}"
}

@Composable
private fun PaneTree(
    node: PaneNode,
    tab: TerminalTab,
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier,
) {
    when (node) {
        is PaneNode.Leaf -> {
            val pane = viewModel.pane(node.paneId) ?: return

            key(node.paneId) {
                TerminalPane(
                    state = pane,
                    focused = node.paneId == tab.focusedPaneId,
                    showFocusBorder = tab.root is PaneNode.Split,
                    onFocus = { viewModel.focusPane(node.paneId) },
                    modifier = modifier,
                )
            }
        }

        is PaneNode.Split -> SplitPane(node, tab, viewModel, modifier)
    }
}

@Composable
private fun SplitPane(
    node: PaneNode.Split,
    tab: TerminalTab,
    viewModel: TerminalViewModel,
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
            PaneTree(node.first, tab, viewModel, Modifier.weight(ratio).fillMaxHeight())
            divider()
            PaneTree(node.second, tab, viewModel, Modifier.weight(1f - ratio).fillMaxHeight())
        }
    } else {
        Column(modifier = modifier.onSizeChanged { size = it }) {
            PaneTree(node.first, tab, viewModel, Modifier.weight(ratio).fillMaxWidth())
            divider()
            PaneTree(node.second, tab, viewModel, Modifier.weight(1f - ratio).fillMaxWidth())
        }
    }
}

