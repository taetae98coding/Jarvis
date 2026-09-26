package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.flow.Flow
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId

internal actual fun zoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int? =
    runCatching { ZoneId.of(zoneId).rules.getOffset(Instant.ofEpochSecond(epochSeconds)).totalSeconds }.getOrNull()

/**
 * `TimeZone.getDefault()` 는 JVM 이 켜질 때 한 번 정하고 시스템 설정이 바뀌어도 그대로다. macOS 는 시스템 설정 →
 * 일반 → 날짜와 시간이 `/etc/localtime` 을 `/var/db/timezone/zoneinfo/<IANA 이름>` 으로 가리키는 심볼릭 링크로
 * 바꾸므로 그 링크를 읽는다. 링크가 없는 OS 에서는 예외 없이 JVM 기본값으로 떨어진다.
 */
internal actual fun systemZoneId(): String =
    runCatching { Files.readSymbolicLink(LocalTimeLink).toString().substringAfter(ZoneInfoMarker, "") }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() && runCatching { ZoneId.of(it) }.isSuccess }
        ?: ZoneId.systemDefault().id

internal actual fun systemZoneChanges(context: PlatformContext): Flow<Unit>? = null

private val LocalTimeLink: Path = Path.of("/etc/localtime")

private const val ZoneInfoMarker = "zoneinfo/"
