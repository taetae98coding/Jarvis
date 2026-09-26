package io.github.taetae98coding.jarvis.data.worldclock

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

internal actual fun zoneOffsetSeconds(zoneId: String, epochSeconds: Long): Int? =
    intlOffsetSeconds(zoneId, epochSeconds * 1000.0).takeUnless(Double::isNaN)?.roundToInt()

internal actual fun systemZoneId(): String = intlSystemZoneId()

// 브라우저에는 시간대 변경 이벤트가 없다(docs/platform/web.html#world-clock).
internal actual fun systemZoneChanges(context: PlatformContext): Flow<Unit>? = null

/*
 * Intl.DateTimeFormat 으로 그 시간대의 벽시계 필드를 받아 UTC 로 다시 조립하고 원래 순간과의 차이를 잰다.
 * timeZoneName: 'longOffset'("GMT+09:00")은 Safari 15.4·Chrome 95 부터라 쓰지 않는다. hourCycle 'h23' 이 없으면
 * 옛 Chrome 이 자정을 "24" 로 준다. Date.UTC 는 0–99 년을 1900 년대로 읽어서 setUTCFullYear 로 연도를 넣는다.
 * 모르는 시간대는 RangeError 라 NaN 을 돌려준다.
 */
private fun intlOffsetSeconds(zoneId: String, epochMillis: Double): Double = js(
    """(() => {
        try {
            const format = new Intl.DateTimeFormat('en-US', {
                timeZone: zoneId, hourCycle: 'h23',
                year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric',
            });
            const fields = {};
            for (const part of format.formatToParts(new Date(epochMillis))) fields[part.type] = Number(part.value);
            const local = new Date(0);
            local.setUTCFullYear(fields.year, fields.month - 1, fields.day);
            local.setUTCHours(fields.hour % 24, fields.minute, fields.second, 0);
            return (local.getTime() - epochMillis) / 1000;
        } catch (e) {
            return NaN;
        }
    })()""",
)

private fun intlSystemZoneId(): String = js("(Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC')")
