package io.github.taetae98coding.jarvis.data.worldclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import java.time.ZoneId

internal actual fun zoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int? =
    runCatching { ZoneId.of(zoneId).rules.getOffset(Instant.ofEpochSecond(epochSeconds)).totalSeconds }.getOrNull()

// ActivityManagerService 는 ACTION_TIMEZONE_CHANGED 를 보내기 전에 모든 앱 프로세스의 TimeZone 기본값을 비운다
// (ActivityThread.updateTimeZone). 그래서 신호를 받고 읽으면 새 시간대다.
internal actual fun systemZoneId(): String = ZoneId.systemDefault().id

internal actual fun systemZoneChanges(context: PlatformContext): Flow<Unit>? {
    val appContext = context.context.applicationContext

    return callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }

        // 보호된 시스템 브로드캐스트는 NOT_EXPORTED 로도 온다. 플래그를 빼면 Android 14 부터 예외다.
        appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED), Context.RECEIVER_NOT_EXPORTED)

        awaitClose { appContext.unregisterReceiver(receiver) }
    }
}
