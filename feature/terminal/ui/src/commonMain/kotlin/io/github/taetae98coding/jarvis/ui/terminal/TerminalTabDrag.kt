package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.DockEdge
import kotlin.math.roundToInt

const val TerminalDragGhostTestTag = "terminal:drag-ghost"

fun terminalDropPreviewTestTag(groupId: Long): String = "terminal:drop-preview:$groupId"

@Immutable
internal data class TerminalDropTarget(val groupId: Long, val edge: DockEdge)

/**
 * 탭을 끄는 동안의 화면 상태. 좌표는 전부 화면 루트([screen])의 로컬 좌표로 맞춘다 — 탭과 그룹이 서로 다른
 * 레이아웃 아래에 있어 각자의 로컬 좌표로는 겹치는지 알 수 없다.
 *
 * 그룹 좌표는 Compose 상태가 아니다. 끄는 동안 그룹의 배치는 바뀌지 않고, 포인터 위치가 바뀔 때마다
 * [target] 이 다시 계산되며 그때의 좌표를 읽으면 된다.
 */
@Stable
internal class TerminalTabDragState {
    private class RegisteredGroup(val tabIds: List<Long>, val coordinates: LayoutCoordinates)

    private val groups = mutableMapOf<Long, RegisteredGroup>()

    var screen: LayoutCoordinates? = null

    var tabId: Long? by mutableStateOf(null)
        private set

    var title: String by mutableStateOf("")
        private set

    var position: Offset by mutableStateOf(Offset.Zero)
        private set

    val dragging: Boolean
        get() = tabId != null

    /** 새 탭 메뉴가 떠 있는지. 끄는 탭처럼 그룹 위로 그려져서 여기 둔다. */
    var menuOpen: Boolean by mutableStateOf(false)

    /**
     * 브라우저 탭의 웹 페이지는 Compose 가 위에 그릴 수 없는 네이티브 뷰다. 그룹 위에 그릴 것(메뉴, 끄는 탭과
     * 놓을 자리 미리 보기)이 있는 동안 페이지를 치운다(docs/platform/jvm.html#terminal-browser).
     */
    val coversPages: Boolean
        get() = dragging || menuOpen

    fun registerGroup(groupId: Long, tabIds: List<Long>, coordinates: LayoutCoordinates) {
        groups[groupId] = RegisteredGroup(tabIds, coordinates)
    }

    fun unregisterGroup(groupId: Long) {
        groups.remove(groupId)
    }

    fun start(tabId: Long, title: String, source: LayoutCoordinates, local: Offset) {
        this.tabId = tabId
        this.title = title
        position = toScreen(source, local)
    }

    fun move(source: LayoutCoordinates, local: Offset) {
        position = toScreen(source, local)
    }

    fun finish() {
        tabId = null
    }

    /**
     * 포인터 아래의 그룹과 그 안의 자리. 놓아도 배치가 같은 자리(자기 그룹의 가운데, 탭 하나뿐인 자기
     * 그룹의 변)는 대상이 아니다.
     */
    fun target(): TerminalDropTarget? {
        val dragging = tabId ?: return null
        val screen = screen ?: return null

        for ((groupId, group) in groups) {
            if (!group.coordinates.isAttached) continue

            val bounds = screen.localBoundingBoxOf(group.coordinates, clipBounds = false)
            if (!bounds.contains(position)) continue

            val edge = edgeAt(bounds, position)
            val own = dragging in group.tabIds
            if (own && (edge == DockEdge.Center || group.tabIds.size == 1)) return null

            return TerminalDropTarget(groupId, edge)
        }

        return null
    }

    private fun toScreen(source: LayoutCoordinates, local: Offset): Offset =
        screen?.takeIf { source.isAttached }?.localPositionOf(source, local) ?: local

    // 거리를 변 길이로 나눠 비교한다. 픽셀 거리로 비교하면 가로로 긴 그룹에서 위·아래 띠가 좌우보다 넓어진다.
    private fun edgeAt(bounds: Rect, point: Offset): DockEdge {
        val x = (point.x - bounds.left) / bounds.width
        val y = (point.y - bounds.top) / bounds.height
        val (edge, distance) = listOf(
            DockEdge.Left to x,
            DockEdge.Right to 1f - x,
            DockEdge.Top to y,
            DockEdge.Bottom to 1f - y,
        ).minBy { it.second }

        return if (distance < TerminalTabDragDefaults.EdgeFraction) edge else DockEdge.Center
    }
}

/**
 * 탭을 끌 수 있게 한다. 마우스는 슬롭을 넘으면, 터치는 길게 누른 뒤에 시작한다 — 터치에서 슬롭만으로
 * 시작하면 탭 줄의 가로 스크롤을 빼앗는다. 시작한 뒤의 이동은 여기서 소비하므로 안쪽 clickable 의
 * 누름이 취소되어 탭이 선택되지 않는다.
 */
@Composable
internal fun Modifier.terminalTabDragSource(
    state: TerminalTabDragState,
    tabId: Long,
    title: String,
    onDrop: (TerminalDropTarget) -> Unit,
): Modifier {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentTitle by rememberUpdatedState(title)
    val currentOnDrop by rememberUpdatedState(onDrop)

    return this
        .onGloballyPositioned { coordinates = it }
        .pointerInput(state, tabId) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val start = when (down.type) {
                    PointerType.Mouse -> awaitMouseDragStart(down)
                    else -> awaitLongPressOrCancellation(down.id)?.also { it.consume() }
                } ?: return@awaitEachGesture
                val source = coordinates ?: return@awaitEachGesture

                state.start(tabId, currentTitle, source, start.position)

                val completed = drag(start.id) { change ->
                    change.consume()
                    state.move(source, change.position)
                }
                val target = state.target()
                state.finish()

                if (completed && target != null) currentOnDrop(target)
            }
        }
}

// awaitTouchSlopOrCancellation 은 마우스에 터치 슬롭의 1/144(0.125dp)만 준다. 그러면 누르는 동안의 1px
// 떨림에도 끌기가 시작돼 탭 선택(클릭)이 사라지므로, 마우스에도 터치 슬롭만큼 움직여야 시작한다.
private suspend fun AwaitPointerEventScope.awaitMouseDragStart(down: PointerInputChange): PointerInputChange? {
    val slop = viewConfiguration.touchSlop

    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return null
        if (!change.pressed || change.isConsumed) return null

        if ((change.position - down.position).getDistance() > slop) {
            change.consume()
            return change
        }
    }
}

/** 포인터를 따라오는 끌던 탭의 이름. 포인터 이벤트를 받지 않도록 화면 루트 위에 마지막으로 그린다. */
@Composable
internal fun TerminalDragGhost(state: TerminalTabDragState, modifier: Modifier = Modifier) {
    if (!state.dragging) return

    val gap = JarvisTheme.dimens.spacing.m

    Text(
        text = state.title,
        style = TerminalTabDefaults.titleStyle,
        color = JarvisTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        modifier = modifier
            .offset {
                val gapPx = gap.roundToPx()
                IntOffset(state.position.x.roundToInt() + gapPx, state.position.y.roundToInt() + gapPx)
            }
            .graphicsLayer { alpha = TerminalTabDragDefaults.GhostAlpha }
            .background(JarvisTheme.colorScheme.primaryContainer, JarvisTheme.shapes.small)
            .padding(horizontal = JarvisTheme.dimens.spacing.m, vertical = JarvisTheme.dimens.spacing.s)
            .testTag(TerminalDragGhostTestTag),
    )
}

/** 놓으면 끌어온 탭이 들어갈 자리. 변이면 그 절반, 가운데면 그룹 전체를 그룹 위에 겹쳐 그린다. */
@Composable
internal fun BoxScope.TerminalDropPreview(groupId: Long, edge: DockEdge) {
    val primary = JarvisTheme.colorScheme.primary
    val alignment = when (edge) {
        DockEdge.Left -> Alignment.CenterStart
        DockEdge.Right -> Alignment.CenterEnd
        DockEdge.Top -> Alignment.TopCenter
        DockEdge.Bottom -> Alignment.BottomCenter
        DockEdge.Center -> Alignment.Center
    }
    val size = when (edge) {
        DockEdge.Left, DockEdge.Right -> Modifier.fillMaxHeight().fillMaxWidth(TerminalTabDragDefaults.PreviewFraction)
        DockEdge.Top, DockEdge.Bottom -> Modifier.fillMaxWidth().fillMaxHeight(TerminalTabDragDefaults.PreviewFraction)
        DockEdge.Center -> Modifier.fillMaxSize()
    }

    Box(
        modifier = Modifier
            .align(alignment)
            .then(size)
            .background(primary.copy(alpha = TerminalTabDragDefaults.PreviewAlpha))
            .border(JarvisTheme.dimens.stroke.thin, primary)
            .testTag(terminalDropPreviewTestTag(groupId)),
    )
}

internal object TerminalTabDragDefaults {
    const val GhostAlpha = 0.85f
    const val PreviewAlpha = 0.25f

    /** 그룹 영역의 네 변에서 안쪽으로 이만큼이 변이고 그 안쪽이 가운데다. */
    const val EdgeFraction = 0.25f

    /** 새 분할의 비율이 0.5 이므로 미리 보기도 절반이다. */
    const val PreviewFraction = 0.5f
}
