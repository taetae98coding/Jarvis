package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.TimeSource

internal const val EmulatorScreenTestTag = "emulator:screen"

// 제스처 테스트가 좌표를 맞추려면 프레임이 그려진 영역을 정확히 집어야 한다. 화면 전체를 누르면
// 레터박스까지 포함되어 기대값이 화면 크기에 따라 달라진다.
internal const val EmulatorFrameTestTag = "emulator:frame"

/**
 * [frames] 는 구독하는 동안에만 흐른다. 이 화면을 벗어나면 수집이 끝나고 기기 화면 촬영도 멈춘다.
 */
@Composable
internal fun EmulatorStreamScreen(
    device: EmulatorDevice,
    frames: Flow<ByteArray?>,
    onGesture: (EmulatorGesture) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var frame by remember(device.id) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(device.id) { mutableStateOf(false) }

    LaunchedEffect(device.id, frames) {
        frames.collect { bytes ->
            // 프레임을 다른 디스패처에서 풀면 브라우저에서 디코딩이 끝나지 않는다. Wasm 은 스레드가
            // 하나뿐이라 Dispatchers.Default 도 같은 이벤트 루프인데, Compose UI 테스트가 그 루프를
            // 점유한 동안 이어지는 코드가 실행되지 못한다. 초당 두 장이라 여기서 바로 푼다.
            val decoded = bytes?.decodeToImageBitmapOrNull()

            if (decoded == null) {
                // 한 장이라도 받아 뒀으면 그걸 계속 보여준다. 빈 화면으로 되돌리지 않는다.
                failed = frame == null
            } else {
                frame = decoded
                failed = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().testTag(EmulatorScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmulatorTopBar(title = device.name, onBack = onBack)

        if (!device.canControl) {
            Text(
                text = "이 기기에는 제스처를 보낼 수 없습니다. 화면만 볼 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val image = frame

            when {
                image != null -> DeviceScreen(
                    image = image,
                    enabled = device.canControl,
                    onGesture = onGesture,
                )

                failed -> Text(text = "화면을 가져올 수 없습니다.")

                else -> Text(text = "화면을 가져오는 중…")
            }
        }
    }
}

/**
 * 프레임의 픽셀 크기가 곧 기기 디스플레이 해상도다. 이미지를 화면에 꽉 채우지 않고 원본 비율로
 * 놓으면, 포인터 좌표를 그린 영역의 크기로 나누는 것만으로 기기 좌표가 나온다. 레터박스를 계산할
 * 필요가 없다.
 */
@Composable
private fun DeviceScreen(
    image: ImageBitmap,
    enabled: Boolean,
    onGesture: (EmulatorGesture) -> Unit,
) {
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .testTag(EmulatorFrameTestTag)
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .pointerInput(image.width, image.height, enabled) {
                if (!enabled) return@pointerInput

                detectTapGestures { offset ->
                    val point = offset.toDevicePixels(size, image)

                    onGesture(EmulatorGesture.Tap(x = point.first, y = point.second))
                }
            }
            .pointerInput(image.width, image.height, enabled) {
                if (!enabled) return@pointerInput

                // detectDragGestures 를 쓰면 시작점이 터치 슬롭을 넘은 지점으로 보고되어, 스와이프가
                // 손가락을 댄 자리보다 몇 픽셀 뒤에서 시작한다. 처음 누른 위치를 그대로 보내려고
                // 제스처를 직접 푼다.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startedAt = TimeSource.Monotonic.markNow()

                    // 슬롭을 넘지 못하면 탭이다. 탭은 옆의 detectTapGestures 가 맡는다.
                    var last = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        ?.position
                        ?: return@awaitEachGesture

                    drag(down.id) { change ->
                        last = change.position
                        change.consume()
                    }

                    val from = down.position.toDevicePixels(size, image)
                    val to = last.toDevicePixels(size, image)

                    onGesture(
                        EmulatorGesture.Swipe(
                            fromX = from.first,
                            fromY = from.second,
                            toX = to.first,
                            toY = to.second,
                            // 사용자가 끈 시간을 그대로 보낸다. 0 에 가까우면 기기가 스와이프가
                            // 아니라 플릭으로 받아 화면이 튕긴다.
                            durationMillis = startedAt.elapsedNow()
                                .inWholeMilliseconds
                                .coerceAtLeast(MinSwipeMillis),
                        ),
                    )
                }
            },
    )
}

private const val MinSwipeMillis = 50L

private fun Offset.toDevicePixels(size: IntSize, image: ImageBitmap): Pair<Int, Int> =
    Pair(
        (x / size.width * image.width).toInt().coerceIn(0, image.width - 1),
        (y / size.height * image.height).toInt().coerceIn(0, image.height - 1),
    )

// 프레임이 깨져 있으면(에이전트가 오류 본문을 PNG 인 척 돌려주면) 디코더가 예외를 던진다. 화면 전체가
// 죽는 것보다 그 프레임을 버리는 편이 낫다.
private fun ByteArray.decodeToImageBitmapOrNull(): ImageBitmap? =
    runCatching { decodeToImageBitmap() }.getOrNull()
