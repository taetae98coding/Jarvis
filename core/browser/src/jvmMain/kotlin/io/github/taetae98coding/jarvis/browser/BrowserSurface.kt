package io.github.taetae98coding.jarvis.browser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Cursor
import kotlin.math.roundToInt

/**
 * [page] 가 그린 픽셀을 그리고, 누르기·움직이기·휠·키를 CDP 로 넘긴다(docs/platform/jvm.html#terminal-browser).
 * Compose 가 그리므로 메뉴·끌기 미리 보기가 페이지 위에 그대로 올라간다.
 *
 * 글자는 보이지 않는 입력칸이 받는다. Compose 는 입력칸이 포커스를 가질 때만 입력기(IME)를 켜서, 한글 조합은 이 길로만
 * 온다. 조합 중인 글자는 `Input.imeSetComposition`, 확정된 글자는 `Input.insertText` 로 보낸다.
 */
@Composable
fun BrowserSurface(
    page: BrowserPage,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val currentOnFocus by rememberUpdatedState(onFocus)
    val input = rememberTextFieldState()
    val focus = remember { FocusRequester() }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var popupImage by remember { mutableStateOf<Pair<java.awt.Rectangle, ImageBitmap>?>(null) }
    val cursor by page.cursor.collectAsState()

    // 버퍼는 몇 장 뒤에 덮어써지므로 받은 자리에서 Skia 로 옮긴다. 옮기는 일은 UI 스레드 밖에서 한다.
    LaunchedEffect(page) {
        page.frame.filterNotNull().collect { frame ->
            val next = withContext(Dispatchers.Default) { frame.toImageBitmap() }
            val previous = image
            image = next
            previous?.release()
        }
    }
    LaunchedEffect(page) {
        page.popup.collect { popup ->
            val next = popup?.let { it.rect to withContext(Dispatchers.Default) { it.frame.toImageBitmap() } }
            val previous = popupImage
            popupImage = next
            previous?.second?.release()
        }
    }
    DisposableEffect(page) {
        onDispose {
            image?.release()
            popupImage?.second?.release()
            image = null
            popupImage = null
        }
    }
    LaunchedEffect(page, input) { forwardText(page, input) }

    Box(
        modifier = modifier
            .onSizeChanged { size -> page.resize((size.width / density).roundToInt(), (size.height / density).roundToInt(), density.toDouble()) }
            .pointerHoverIcon(cursor.toPointerIcon())
            .pointerInput(page) {
                var lastPress = 0L
                var lastPosition = Offset.Zero
                var clickCount = 0
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val x = (change.position.x / density).roundToInt()
                        val y = (change.position.y / density).roundToInt()
                        val modifiers = event.keyboardModifiers.cdpModifiers()

                        when (event.type) {
                            PointerEventType.Press -> {
                                focus.requestFocus()
                                currentOnFocus()
                                val now = change.uptimeMillis
                                clickCount = if (now - lastPress < DoubleClickMillis && (change.position - lastPosition).getDistance() < DoubleClickSlop) clickCount + 1 else 1
                                lastPress = now
                                lastPosition = change.position
                                val button = if (event.buttons.isSecondaryPressed) "right" else "left"
                                page.send("Input.dispatchMouseEvent", mouseEvent("mousePressed", x, y, button, clickCount, modifiers))
                            }
                            PointerEventType.Release -> {
                                page.send("Input.dispatchMouseEvent", mouseEvent("mouseReleased", x, y, "left", clickCount, modifiers))
                            }
                            PointerEventType.Move -> {
                                val button = if (event.buttons.isPrimaryPressed) "left" else "none"
                                page.send("Input.dispatchMouseEvent", mouseEvent("mouseMoved", x, y, button, 0, modifiers))
                            }
                            PointerEventType.Scroll -> {
                                val delta = change.scrollDelta
                                page.send(
                                    "Input.dispatchMouseEvent",
                                    buildJsonObject {
                                        put("type", "mouseWheel")
                                        put("x", x)
                                        put("y", y)
                                        // Compose 의 한 칸은 1.0 이다. 브라우저의 한 칸(약 100px)에 맞춘다.
                                        put("deltaX", delta.x * WheelStep)
                                        put("deltaY", delta.y * WheelStep)
                                        put("modifiers", modifiers)
                                    },
                                )
                            }
                            PointerEventType.Exit -> Unit
                            else -> Unit
                        }
                        change.consume()
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bitmap = image ?: return@Canvas
            drawImage(bitmap, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
            popupImage?.let { (rect, popup) ->
                drawImage(
                    popup,
                    dstOffset = IntOffset((rect.x * density).roundToInt(), (rect.y * density).roundToInt()),
                    dstSize = IntSize((rect.width * density).roundToInt(), (rect.height * density).roundToInt()),
                )
            }
        }

        BasicTextField(
            state = input,
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focus)
                .onFocusChanged { page.setFocus(it.isFocused) }
                .onPreviewKeyEvent { event -> handleKey(page, input, event) },
        )
    }
}

/**
 * 조합이 없으면 입력칸의 글자는 모두 확정된 것이다. 조합이 있으면 그 앞까지가 확정이고, 조합 부분은 페이지에 조합으로
 * 보인다. 이미 보낸 확정 글자 수를 세어 두고 새로 늘어난 것만 보낸다. 조합이 끝나 확정만 남으면 입력칸을 비운다.
 */
private suspend fun forwardText(page: BrowserPage, input: TextFieldState) {
    var sent = 0
    var composing = false

    snapshotFlow { input.text.toString() to input.composition }.collect { (text, composition) ->
        val committed = if (composition == null) text else text.substring(0, composition.min)
        if (committed.length > sent) {
            page.send("Input.insertText", buildJsonObject { put("text", committed.substring(sent)) })
            sent = committed.length
        }

        if (composition != null) {
            val composed = text.substring(composition.min, composition.max)
            page.send(
                "Input.imeSetComposition",
                buildJsonObject {
                    put("text", composed)
                    put("selectionStart", composed.length)
                    put("selectionEnd", composed.length)
                },
            )
            composing = true
        } else {
            if (composing) {
                // 조합이 지워지고 끝났다(조합 중 Esc 등). 페이지의 조합도 치운다.
                page.send("Input.imeSetComposition", buildJsonObject { put("text", ""); put("selectionStart", 0); put("selectionEnd", 0) })
                composing = false
            }
            if (text.isNotEmpty()) {
                input.clearText()
                sent = 0
            }
        }
    }
}

// 글자가 아닌 키. 조합 중이면 입력기에 둔다(조합 중 Backspace 는 자모를 지운다). ⌘ 조합은 편집 명령만 먹고
// 나머지(⌘T·⌘W 같은 터미널 단축키)는 흘려보낸다.
private fun handleKey(page: BrowserPage, input: TextFieldState, event: KeyEvent): Boolean {
    if (input.composition != null) return false

    if (event.isMetaPressed) {
        val command = when (event.key) {
            Key.C -> EditCommand.Copy
            Key.X -> EditCommand.Cut
            Key.V -> EditCommand.Paste
            Key.A -> EditCommand.SelectAll
            Key.Z -> if (event.isShiftPressed) EditCommand.Redo else EditCommand.Undo
            else -> return false
        }
        if (event.type == KeyEventType.KeyDown) page.edit(command)
        return true
    }

    val spec = ComposeKeys[event.key] ?: return false
    val modifiers = (if (event.isAltPressed) 1 else 0) or (if (event.isCtrlPressed) 2 else 0) or (if (event.isShiftPressed) 8 else 0)
    when (event.type) {
        KeyEventType.KeyDown -> {
            page.send("Input.dispatchKeyEvent", spec.event("rawKeyDown", modifiers))
            spec.text?.let { text -> page.send("Input.dispatchKeyEvent", buildJsonObject { put("type", "char"); put("text", text); put("modifiers", modifiers) }) }
        }
        KeyEventType.KeyUp -> page.send("Input.dispatchKeyEvent", spec.event("keyUp", modifiers))
        else -> Unit
    }
    return true
}

private val ComposeKeys: Map<Key, CdpKey> = mapOf(
    Key.Enter to CdpKeys.getValue("Enter"),
    Key.NumPadEnter to CdpKeys.getValue("Enter"),
    Key.Tab to CdpKeys.getValue("Tab"),
    Key.Backspace to CdpKeys.getValue("Backspace"),
    Key.Delete to CdpKeys.getValue("Delete"),
    Key.Escape to CdpKeys.getValue("Escape"),
    Key.DirectionUp to CdpKeys.getValue("ArrowUp"),
    Key.DirectionDown to CdpKeys.getValue("ArrowDown"),
    Key.DirectionLeft to CdpKeys.getValue("ArrowLeft"),
    Key.DirectionRight to CdpKeys.getValue("ArrowRight"),
    Key.MoveHome to CdpKeys.getValue("Home"),
    Key.MoveEnd to CdpKeys.getValue("End"),
    Key.PageUp to CdpKeys.getValue("PageUp"),
    Key.PageDown to CdpKeys.getValue("PageDown"),
)

private fun mouseEvent(type: String, x: Int, y: Int, button: String, clickCount: Int, modifiers: Int) =
    buildJsonObject {
        put("type", type)
        put("x", x)
        put("y", y)
        put("button", button)
        put("clickCount", clickCount)
        put("modifiers", modifiers)
    }

// CDP 의 modifiers 비트: Alt 1, Ctrl 2, Meta 4, Shift 8.
private fun PointerKeyboardModifiers.cdpModifiers(): Int =
    (if (isAltPressed) 1 else 0) or (if (isCtrlPressed) 2 else 0) or (if (isMetaPressed) 4 else 0) or (if (isShiftPressed) 8 else 0)

private fun Int.toPointerIcon(): PointerIcon =
    when (this) {
        Cursor.HAND_CURSOR -> PointerIcon.Hand
        Cursor.TEXT_CURSOR -> PointerIcon.Text
        Cursor.CROSSHAIR_CURSOR -> PointerIcon.Crosshair
        else -> PointerIcon.Default
    }

// 엔진의 BGRA 를 그대로 설치한다. installPixels 가 픽셀을 복사하므로 돌려 쓰는 버퍼를 넘겨도 된다
// (기기 화면의 FrameImage.skiko.kt 와 같은 방식).
private fun BrowserFrame.toImageBitmap(): ImageBitmap {
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
    val rowBytes = width * 4

    return Bitmap().apply {
        check(installPixels(info, pixels, rowBytes)) { "BGRA 픽셀을 설치하지 못했다" }
        setImmutable()
    }.asComposeImageBitmap()
}

// 네이티브 픽셀을 곧바로 놓는다. 초당 60장 × 수 MB 가 GC 를 기다리며 쌓이지 않게 한다.
private fun ImageBitmap.release() {
    asSkiaBitmap().close()
}

private const val DoubleClickMillis = 400L
private const val DoubleClickSlop = 8f
private const val WheelStep = 100f
