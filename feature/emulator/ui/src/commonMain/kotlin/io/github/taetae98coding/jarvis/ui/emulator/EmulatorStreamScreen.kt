package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.TouchAction
import kotlinx.coroutines.withContext

const val EmulatorScreenTestTag = "emulator:screen"

// 제스처 테스트가 좌표를 맞추려면 프레임이 그려진 영역을 정확히 집어야 한다. 화면 전체를 누르면
// 레터박스까지 포함되어 기대값이 화면 크기에 따라 달라진다.
const val EmulatorFrameTestTag = "emulator:frame"

const val EmulatorScreenWakeTestTag = "emulator:screen:wake"

/**
 * 기기 목록에서 고른 기기의 화면. 상단 막대 아래는 [EmulatorStream] 이라 터미널의 기기 탭과 같다.
 */
@Composable
internal fun EmulatorStreamScreen(
    viewModel: EmulatorScreenViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val device by viewModel.device.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().testTag(EmulatorScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
    ) {
        // 목록이 첫 답을 하기 전에는 이름을 모른다. 그 사이에는 제목 없이 뒤로만 보여준다.
        JarvisTopBar(title = device?.name.orEmpty(), onBack = onBack)

        EmulatorStream(viewModel = viewModel, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * 기기 화면 하나. 프레임은 수집하는 동안에만 흐르고, 컴포지션에서 빠지거나 앱이 백그라운드로 가면 촬영도 멈춘다.
 *
 * 마지막 프레임은 [retain] 이 들고 있어서 컴포지션이 다시 만들어져도 빈 화면으로 돌아가지 않는다.
 * 무엇을 어디에 두는지는 docs/common/retained-state.html 에 있다.
 */
@Composable
internal fun EmulatorStream(
    viewModel: EmulatorScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val device by viewModel.device.collectAsStateWithLifecycle()
    val isWaking by viewModel.isWaking.collectAsStateWithLifecycle()
    val frame = retain(viewModel.deviceId) { EmulatorFrameState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 앱이 백그라운드로 가면 촬영을 멈춘다. 화면은 남아 있어서 LaunchedEffect 만으로는 수집이 끝나지 않는다.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.frames.collect { next ->
                val decoded = next?.let { withContext(FrameDecodeContext) { it.toImageBitmapOrNull() } }

                // 한 장이라도 받아 뒀으면 그걸 계속 보여준다. 빈 화면으로 되돌리지 않는다.
                if (decoded == null) frame.markFailed() else frame.updateImage(decoded)
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
    ) {
        if (device?.canControl == false) {
            Text(
                text = "이 기기에는 제스처를 보낼 수 없습니다. 화면만 볼 수 있습니다.",
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val image = frame.image

            when {
                image != null -> DeviceScreen(
                    image = image,
                    enabled = device?.canControl == true,
                    onGesture = viewModel::onGesture,
                )

                frame.failed -> Text(text = "화면을 가져올 수 없습니다.")

                else -> Text(text = "화면을 가져오는 중…")
            }

            // 프레임 위에 겹친다. 꺼진 화면은 검은 그림이라 가릴 것이 없고, 사용자의 눈이 이미 거기에 있다.
            if (device?.isAsleep == true && device?.canControl == true) {
                WakePrompt(isWaking = isWaking, onWake = viewModel::onWake)
            }
        }
    }
}

@Composable
private fun WakePrompt(
    isWaking: Boolean,
    onWake: () -> Unit,
) {
    JarvisCard(modifier = Modifier.padding(JarvisTheme.dimens.layout.screenPadding)) {
        Text(
            text = "기기 화면이 꺼져 있습니다.",
            style = JarvisTheme.typography.titleMedium,
        )

        if (isWaking) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(EmulatorDeviceDefaults.progressSize),
                    strokeWidth = EmulatorDeviceDefaults.progressStrokeWidth,
                )
                Text(
                    text = "화면을 켜는 중…",
                    style = JarvisTheme.typography.bodyMedium,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            FilledTonalButton(
                onClick = onWake,
                modifier = Modifier.testTag(EmulatorScreenWakeTestTag),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                // 글자가 같은 것을 말하므로 아이콘은 읽지 않는다.
                Icon(
                    imageVector = JarvisIcons.Sun,
                    contentDescription = null,
                    modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = "화면 켜기")
            }
        }
    }
}

/**
 * 프레임의 픽셀 크기가 곧 기기 디스플레이 해상도가 아니라, 지금 보고 있는 영상 프레임의 크기다. 그 크기를
 * 제스처에 함께 실어 보내면 기기 쪽 에이전트가 디스플레이 좌표로 되돌린다(docs/common/device-mirroring.html).
 * 이미지를 원본 비율로 놓으므로 포인터 좌표를 그린 영역 크기로 나누면 프레임 좌표가 나온다.
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
                // 이벤트를 분류하지 않고 누름·이동·뗌을 그대로 흘려보낸다. 탭인지 스와이프인지, 길게
                // 누른 것인지는 기기가 가른다. 안 눌린 마우스 이동은 호버로 간다.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        val point = change.position.toDevicePixels(size, image)

                        when (event.type) {
                            PointerEventType.Press ->
                                if (enabled) {
                                    onGesture(touch(TouchAction.DOWN, point, image))
                                    change.consume()
                                }

                            // 누른 채 움직이면 드래그. 마우스가 눌리지 않고 움직이거나(Move) 화면에
                            // 들어오면(Enter) 호버로 간다 — 기기 안 UI 가 호버 상태를 그린다.
                            PointerEventType.Move, PointerEventType.Enter ->
                                when {
                                    change.pressed ->
                                        if (enabled && event.type == PointerEventType.Move) {
                                            onGesture(touch(TouchAction.MOVE, point, image))
                                            change.consume()
                                        }

                                    enabled && change.type == PointerType.Mouse ->
                                        onGesture(
                                            EmulatorGesture.Hover(
                                                x = point.first,
                                                y = point.second,
                                                frameWidth = image.width,
                                                frameHeight = image.height,
                                            ),
                                        )
                                }

                            PointerEventType.Release ->
                                if (enabled) {
                                    onGesture(touch(TouchAction.UP, point, image))
                                    change.consume()
                                }
                        }
                    }
                }
            },
    )
}

private fun touch(action: TouchAction, point: Pair<Int, Int>, image: ImageBitmap): EmulatorGesture.Touch =
    EmulatorGesture.Touch(
        action = action,
        x = point.first,
        y = point.second,
        frameWidth = image.width,
        frameHeight = image.height,
    )

private fun Offset.toDevicePixels(size: IntSize, image: ImageBitmap): Pair<Int, Int> =
    Pair(
        (x / size.width * image.width).toInt().coerceIn(0, image.width - 1),
        (y / size.height * image.height).toInt().coerceIn(0, image.height - 1),
    )
