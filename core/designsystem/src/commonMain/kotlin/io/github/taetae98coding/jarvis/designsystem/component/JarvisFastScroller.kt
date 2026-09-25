package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** [JarvisFastScroller] 가 읽고 옮기는 스크롤. 값은 모두 스냅샷 상태에서 읽어 바뀌면 막대가 다시 그려진다. */
@Stable
interface JarvisFastScrollerAdapter {
    val scrollable: Boolean

    /** 보이는 높이 ÷ 전체 높이. 0..1. */
    val visibleRatio: Float

    /** 스크롤한 비율. 0 은 맨 위, 1 은 맨 아래다. */
    val fraction: Float

    suspend fun scrollToFraction(fraction: Float)
}

@Composable
fun rememberFastScrollerAdapter(state: LazyListState): JarvisFastScrollerAdapter = remember(state) { LazyListFastScrollerAdapter(state) }

@Composable
fun rememberFastScrollerAdapter(state: ScrollState): JarvisFastScrollerAdapter = remember(state) { ScrollStateFastScrollerAdapter(state) }

/**
 * 목록 오른쪽 끝에 겹쳐 두는 빠른 스크롤 막대(docs/common/terminal-file-editor.html S1–S4). 손잡이를 끌거나 막대를 누르면
 * 그 비율의 자리로 곧장 옮긴다. 내용이 다 보이면 아무것도 그리지 않는다.
 */
@Composable
fun JarvisFastScroller(
    adapter: JarvisFastScrollerAdapter,
    modifier: Modifier = Modifier,
) {
    if (!adapter.scrollable) return

    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val active = hovered || dragging
    val thumbWidth by animateDpAsState(if (active) JarvisFastScrollerDefaults.activeThumbWidth else JarvisFastScrollerDefaults.thumbWidth)
    val color = JarvisTheme.colorScheme.onSurfaceVariant.copy(alpha = if (active) 0.7f else 0.38f)
    val currentAdapter by rememberUpdatedState(adapter)

    fun thumb(trackHeight: Float, minThumb: Float): Pair<Float, Float> {
        val height = (trackHeight * currentAdapter.visibleRatio).coerceIn(minOf(minThumb, trackHeight), trackHeight)
        return (trackHeight - height) * currentAdapter.fraction.coerceIn(0f, 1f) to height
    }

    Box(
        modifier = modifier
            .width(JarvisFastScrollerDefaults.touchWidth)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                val minThumb = JarvisFastScrollerDefaults.minThumbHeight.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val trackHeight = size.height.toFloat()
                    val (top, height) = thumb(trackHeight, minThumb)

                    fun moveThumbTo(thumbTop: Float) {
                        val range = trackHeight - height
                        val fraction = if (range <= 0f) 0f else (thumbTop / range).coerceIn(0f, 1f)
                        scope.launch { currentAdapter.scrollToFraction(fraction) }
                    }

                    // 손잡이 밖을 눌렀으면 손잡이 가운데를 그 자리로 옮기고, 그 자리를 잡은 채 끈다.
                    var grab = down.position.y - top
                    if (grab !in 0f..height) {
                        grab = height / 2
                        moveThumbTo(down.position.y - grab)
                    }

                    dragging = true
                    try {
                        drag(down.id) { change ->
                            change.consume()
                            moveThumbTo(change.position.y - grab)
                        }
                    } finally {
                        dragging = false
                    }
                }
            }
            .drawBehind {
                val (top, height) = thumb(size.height, JarvisFastScrollerDefaults.minThumbHeight.toPx())
                val width = thumbWidth.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width - width - JarvisFastScrollerDefaults.edgePadding.toPx(), top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(width / 2),
                )
            },
    )
}

object JarvisFastScrollerDefaults {
    val touchWidth: Dp = 16.dp

    val thumbWidth: Dp = 4.dp

    val activeThumbWidth: Dp = 8.dp

    val minThumbHeight: Dp = 32.dp

    val edgePadding: Dp = 2.dp
}

// LazyColumn 은 보이지 않는 항목의 높이를 모른다. 보이는 항목의 평균 높이 × 항목 수로 전체를 어림한다.
private class LazyListFastScrollerAdapter(private val state: LazyListState) : JarvisFastScrollerAdapter {
    private val averageItemSize: Float
        get() {
            val info = state.layoutInfo
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return 0f

            return items.sumOf { it.size }.toFloat() / items.size + info.mainAxisItemSpacing
        }

    private val viewportSize: Float
        get() = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset).toFloat()

    private val contentSize: Float
        get() = averageItemSize * state.layoutInfo.totalItemsCount + state.layoutInfo.beforeContentPadding + state.layoutInfo.afterContentPadding

    override val scrollable: Boolean
        get() = state.canScrollForward || state.canScrollBackward

    override val visibleRatio: Float
        get() = if (contentSize <= 0f) 1f else (viewportSize / contentSize).coerceIn(0f, 1f)

    // 끝에 닿았는지는 어림이 아니라 목록이 아는 값으로 정해서, 맨 위·맨 아래에서 손잡이가 끝에 붙게 한다.
    override val fraction: Float
        get() = when {
            !state.canScrollBackward -> 0f
            !state.canScrollForward -> 1f
            else -> {
                val range = contentSize - viewportSize
                if (range <= 0f) 0f else ((state.firstVisibleItemIndex * averageItemSize + state.firstVisibleItemScrollOffset) / range).coerceIn(0f, 1f)
            }
        }

    override suspend fun scrollToFraction(fraction: Float) {
        val total = state.layoutInfo.totalItemsCount
        val average = averageItemSize
        if (total == 0 || average <= 0f) return
        if (fraction >= 1f) {
            state.scrollToItem(total - 1)
            return
        }

        val target = fraction * (contentSize - viewportSize).coerceAtLeast(0f)
        val index = (target / average).toInt().coerceIn(0, total - 1)
        state.scrollToItem(index, (target - index * average).roundToInt())
    }
}

private class ScrollStateFastScrollerAdapter(private val state: ScrollState) : JarvisFastScrollerAdapter {
    override val scrollable: Boolean
        get() = state.maxValue in 1 until Int.MAX_VALUE

    override val visibleRatio: Float
        get() = state.viewportSize.toFloat() / (state.viewportSize + state.maxValue).coerceAtLeast(1)

    override val fraction: Float
        get() = if (state.maxValue <= 0) 0f else state.value.toFloat() / state.maxValue

    override suspend fun scrollToFraction(fraction: Float) {
        state.scrollTo((fraction * state.maxValue).roundToInt())
    }
}
