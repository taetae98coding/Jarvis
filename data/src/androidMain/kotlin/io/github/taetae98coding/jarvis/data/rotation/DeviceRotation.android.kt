package io.github.taetae98coding.jarvis.data.rotation

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlin.time.Duration.Companion.seconds

internal actual fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource =
    SystemRotation(context.context)

/**
 * `Settings.System` 의 `ACCELEROMETER_ROTATION` 과 `USER_ROTATION` 을 읽고 쓴다.
 *
 * Activity 의 `requestedOrientation` 과 달리 기기 전역 값이라 홈 화면과 다른 앱도 따라 돈다.
 * 대신 사용자가 설정 화면에서 직접 허용하는 `WRITE_SETTINGS` 가 필요하다.
 */
private class SystemRotation(private val context: Context) : DeviceRotationDataSource {
    private val resolver: ContentResolver = context.contentResolver

    private val displayManager: DisplayManager? =
        context.getSystemService(DisplayManager::class.java)

    // 권한 상태는 시스템이 알려주지 않는다. 시스템 전역 화면 유지와 같은 이유로 짧은 간격으로 다시 본다.
    private val permissions: Flow<Boolean> =
        observeByPolling(interval = PermissionPollInterval) { isPermitted() }

    // 각도는 DisplayManager 가, 잠금은 ContentObserver 가 알려준다. 둘 다 콜백이 있어 폴링하지 않는다.
    private val rotations: Flow<Rotation> =
        observeOnSignals(signals = merge(displayChanges(), lockChanges())) { readRotation() }

    override fun readStatus(): DeviceRotationStatus = status(isPermitted(), readRotation())

    override fun observeStatus(): Flow<DeviceRotationStatus> =
        combine(permissions, rotations, ::status)

    override fun setAngle(angle: RotationAngle) {
        write(Settings.System.USER_ROTATION, angle.degrees / 90)
    }

    override fun setLocked(locked: Boolean) {
        write(Settings.System.ACCELEROMETER_ROTATION, if (locked) 0 else 1)
    }

    override fun requestPermission() {
        if (isPermitted()) return

        // 애플리케이션 컨텍스트로 띄우므로 새 태스크가 필요하다. WRITE_SETTINGS 는 런타임 권한
        // 다이얼로그가 아니라 설정 화면에서만 허용할 수 있다.
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
    }

    private fun status(permitted: Boolean, rotation: Rotation) =
        DeviceRotationStatus(
            supported = true,
            permitted = permitted,
            locked = rotation.locked,
            angle = rotation.angle,
        )

    private fun readRotation(): Rotation = Rotation(locked = isLocked(), angle = readAngle())

    // 자동 회전 중에는 USER_ROTATION 이 갱신되지 않는다. 지금 실제 각도는 디스플레이에서 읽어야 한다.
    private fun readAngle(): RotationAngle? {
        val rotation = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: return null

        return RotationAngle.ofDegrees(rotation * 90)
    }

    private fun isLocked(): Boolean = read(Settings.System.ACCELEROMETER_ROTATION) == 0

    private fun isPermitted(): Boolean = Settings.System.canWrite(context)

    private fun read(key: String): Int? =
        // 키가 없으면 SettingNotFoundException 을 던진다.
        runCatching { Settings.System.getInt(resolver, key) }.getOrNull()

    private fun write(key: String, value: Int) {
        if (!isPermitted()) return

        // canWrite 를 확인한 뒤에도 그 사이 권한이 회수되면 SecurityException 이 난다.
        runCatching { Settings.System.putInt(resolver, key, value) }
    }

    private fun displayChanges(): Flow<Unit> =
        callbackFlow {
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = Unit

                override fun onDisplayRemoved(displayId: Int) = Unit

                override fun onDisplayChanged(displayId: Int) {
                    trySend(Unit)
                }
            }

            // Handler 를 주지 않으면 수집 중인 스레드의 Looper 를 쓰는데, Flow 는 Looper 가 없는
            // 스레드에서도 수집된다.
            displayManager?.registerDisplayListener(listener, Handler(Looper.getMainLooper()))

            awaitClose { displayManager?.unregisterDisplayListener(listener) }
        }

    private fun lockChanges(): Flow<Unit> =
        callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                observer,
            )

            awaitClose { resolver.unregisterContentObserver(observer) }
        }
}

private data class Rotation(val locked: Boolean, val angle: RotationAngle?)

private val PermissionPollInterval = 2.seconds
