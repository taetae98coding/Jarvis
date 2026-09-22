package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import kotlinx.browser.window
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal actual fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource =
    ScreenOrientationRotation

/**
 * Screen Orientation API 로 브라우저 문서를 돌린다.
 *
 * `lock()` 이 잠금과 회전을 한 번에 한다. 각도가 아니라 방향 이름을 받으므로 지금 방향에서
 * natural 이 세로인지 가로인지 역산해서 이름을 고른다.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private object ScreenOrientationRotation : DeviceRotationDataSource {
    // orientation.type 은 잠겼는지 알려주지 않는다. 전체화면을 벗어나면 브라우저가 잠금을 풀지만
    // 그 사실도 알려주지 않아서, 우리가 건 잠금만 기억한다.
    private var locked = false

    // lock() 은 예외가 아니라 거절로 실패한다. 실패했다는 사실 자체가 화면에 보여야 하는 상태라서
    // 붙잡아 둔다. 데스크탑 브라우저와 전체화면이 아닌 Chrome/Android 가 여기로 떨어진다.
    private var rejected = false

    private val rejections = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun readStatus(): DeviceRotationStatus {
        val orientation = screenOrientationOrNull()
            ?: return DeviceRotationStatus(supported = false)

        return DeviceRotationStatus(
            supported = true,
            permitted = !rejected,
            locked = locked,
            angle = RotationAngle.ofDegrees(orientation.angle),
        )
    }

    override fun observeStatus(): Flow<DeviceRotationStatus> =
        observeOnSignals(signals = merge(orientationChanges(), rejections)) { readStatus() }

    override fun setAngle(angle: RotationAngle) {
        val orientation = screenOrientationOrNull() ?: return

        lock(orientation, angle)
    }

    override fun setLocked(locked: Boolean) {
        val orientation = screenOrientationOrNull() ?: return

        if (locked) {
            lock(orientation, RotationAngle.ofDegrees(orientation.angle) ?: RotationAngle.Degrees0)
        } else {
            this.locked = false
            orientation.unlock()
            rejected = false
            rejections.tryEmit(Unit)
        }
    }

    // 브라우저 정책은 사용자가 허용할 수 있는 권한이 아니다. 열어 줄 설정 화면이 없다.
    override fun requestPermission() = Unit


    private fun lock(orientation: ScreenOrientation, angle: RotationAngle) {
        locked = true
        rejected = false

        orientation.lock(orientation.orientationTypeOf(angle))
            .catch { _ ->
                rejected = true
                locked = false
                rejections.tryEmit(Unit)
                null
            }
    }

    private fun orientationChanges(): Flow<Unit> =
        callbackFlow {
            val listener: (Event) -> Unit = { trySend(Unit) }

            // screen.orientation 의 change 이벤트를 직접 듣는 편이 정확하지만, 리스너를 JS 쪽으로
            // 넘기려면 글루 함수가 필요하다. window 의 orientationchange 가 같은 시점에 온다.
            window.addEventListener("orientationchange", listener)

            awaitClose { window.removeEventListener("orientationchange", listener) }
        }
}

/**
 * 목표 각도를 `lock()` 이 받는 방향 이름으로 바꾼다.
 *
 * natural 방향은 API 가 알려주지 않아 지금 값에서 역산한다. 이름이 세로인데 각도가 90·270 이면
 * natural 은 가로다.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun ScreenOrientation.orientationTypeOf(angle: RotationAngle): String {
    val quarterTurned = this.angle == 90 || this.angle == 270
    val naturallyPortrait = type.startsWith("portrait") != quarterTurned

    val portrait = when (angle) {
        RotationAngle.Degrees0, RotationAngle.Degrees180 -> naturallyPortrait
        RotationAngle.Degrees90, RotationAngle.Degrees270 -> !naturallyPortrait
    }

    val primary = when (angle) {
        RotationAngle.Degrees0, RotationAngle.Degrees90 -> true
        RotationAngle.Degrees180, RotationAngle.Degrees270 -> false
    }

    return "${if (portrait) "portrait" else "landscape"}-${if (primary) "primary" else "secondary"}"
}

// kotlinx-browser 0.5.0 의 org.w3c.dom.Screen 에는 orientation 이 없다.
@OptIn(ExperimentalWasmJsInterop::class)
private external interface ScreenOrientation : JsAny {
    val angle: Int
    val type: String

    fun lock(orientation: String): Promise<JsAny?>

    fun unlock()
}

// lock 이 없는 브라우저(iOS Safari)가 있어서 존재 여부까지 확인한다.
@OptIn(ExperimentalWasmJsInterop::class)
private fun screenOrientationOrNull(): ScreenOrientation? =
    js("(typeof screen !== 'undefined' && screen.orientation && typeof screen.orientation.lock === 'function') ? screen.orientation : null")
