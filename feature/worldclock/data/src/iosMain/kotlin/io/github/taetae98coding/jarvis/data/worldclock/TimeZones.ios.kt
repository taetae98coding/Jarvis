package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSystemTimeZoneDidChangeNotification
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.resetSystemTimeZone
import platform.Foundation.systemTimeZone
import platform.Foundation.timeZoneWithName

internal actual fun zoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int? =
    NSTimeZone.timeZoneWithName(zoneId)
        ?.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble()))
        ?.toInt()

internal actual fun systemZoneId(): String {
    // systemTimeZone 은 처음 읽은 값을 캐시한다. 비우지 않으면 설정을 바꿔도 옛 시간대다.
    NSTimeZone.resetSystemTimeZone()
    return NSTimeZone.systemTimeZone.name
}

internal actual fun systemZoneChanges(context: PlatformContext): Flow<Unit>? = callbackFlow {
    val center = NSNotificationCenter.defaultCenter
    val observer = center.addObserverForName(name = NSSystemTimeZoneDidChangeNotification, `object` = null, queue = null) { trySend(Unit) }

    awaitClose { center.removeObserver(observer) }
}
