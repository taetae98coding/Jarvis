package io.github.taetae98coding.jarvis.data.screen

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.data.state.observeWriteSettingsPermission
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal actual fun createSystemScreenAwakeDataSource(context: PlatformContext): SystemScreenAwakeDataSource =
    ScreenOffTimeout(context.context)

/**
 * `Settings.System.SCREEN_OFF_TIMEOUT` 을 읽고 쓴다.
 *
 * 앱 윈도우에 붙는 `FLAG_KEEP_SCREEN_ON` 과 달리 시스템 전역 값이라 앱이 없어도 유지된다. 대신
 * 사용자가 설정 화면에서 직접 허용해야 하는 `WRITE_SETTINGS` 가 필요하고, 남의 설정을 바꾸는
 * 것이므로 되돌릴 값을 우리가 보관할 책임이 있다.
 */
private class ScreenOffTimeout(private val context: Context) : SystemScreenAwakeDataSource {
    private val resolver: ContentResolver = context.contentResolver

    // 되돌릴 값은 Int 라 boolean 만 다루는 SettingsStore 에 넣을 수 없고, 이 기능이 있는 플랫폼도
    // Android 뿐이어서 여기서만 쓰는 파일에 따로 보관한다.
    private val saved: SharedPreferences =
        context.getSharedPreferences("jarvis.screen_off_timeout", Context.MODE_PRIVATE)

    private val permissions: Flow<Boolean> = observeWriteSettingsPermission(context)

    // 값은 ContentObserver 가 알려준다. 시스템 설정 앱에서 바꿔도 즉시 반영된다.
    private val timeouts: Flow<Duration?> =
        observeOnSignals(signals = timeoutChanges()) { readTimeout() }

    override fun readStatus(): SystemScreenAwakeStatus = status(isPermitted(), readTimeout())

    override fun observeStatus(): Flow<SystemScreenAwakeStatus> =
        combine(permissions, timeouts, ::status)

    override fun setEnabled(enabled: Boolean) {
        if (enabled) extend() else restore()
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

    private fun status(permitted: Boolean, screenOffTimeout: Duration?) =
        SystemScreenAwakeStatus(
            supported = true,
            permitted = permitted,
            screenOffTimeout = screenOffTimeout,
        )

    private fun extend() {
        if (!isPermitted()) return

        val current = rawTimeout() ?: return

        // 앱을 껐다 켜서 다시 불려도 이미 늘려 둔 값을 원본으로 덮어쓰지 않는다.
        if (!saved.contains(RestoreToKey)) {
            saved.edit().putInt(RestoreToKey, current).apply()
        }

        write(ExtendedTimeoutMillis)
    }

    private fun restore() {
        val restoreTo = saved.getInt(RestoreToKey, NoSavedTimeout)

        // 권한을 잃으면 되돌릴 수 없다. 저장값은 남겨 둬서 권한이 돌아오면 복원할 수 있게 한다.
        if (restoreTo == NoSavedTimeout || !isPermitted()) return

        write(restoreTo)
        saved.edit().remove(RestoreToKey).apply()
    }

    private fun isPermitted(): Boolean = Settings.System.canWrite(context)

    private fun readTimeout(): Duration? = rawTimeout()?.milliseconds

    private fun rawTimeout(): Int? =
        // 키가 없으면 SettingNotFoundException 을 던진다.
        runCatching { Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT) }.getOrNull()

    private fun write(millis: Int) {
        // canWrite 를 확인한 뒤에도 그 사이 권한이 회수되면 SecurityException 이 난다.
        runCatching { Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, millis) }
    }

    private fun timeoutChanges(): Flow<Unit> =
        callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
                false,
                observer,
            )

            awaitClose { resolver.unregisterContentObserver(observer) }
        }
}

private const val RestoreToKey = "restore_to"
private const val NoSavedTimeout = -1

// Int.MAX_VALUE ms 는 약 24.8일이다. 설정 앱의 선택지(보통 최대 30분)보다 크지만 값 자체에 상한은
// 없다. 제조사 ROM 이 자체적으로 줄이는 경우까지는 막을 수 없다.
private const val ExtendedTimeoutMillis = Int.MAX_VALUE
