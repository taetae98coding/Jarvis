package io.github.taetae98coding.jarvis.ui.profiling

import kotlin.math.abs
import kotlin.math.roundToLong

/** 0–100 으로 잘라 정수로 적는다. */
internal fun formatPercent(percent: Double): String = "${percent.coerceIn(0.0, 100.0).roundToLong()}%"

/** 1000 단위(macOS·Android 의 파일 크기 표기와 같다). 100 미만이면 소수 한 자리까지 적고, `.0` 은 뗀다. */
internal fun formatBytes(bytes: Long): String {
    var value = bytes.toDouble()
    var unit = 0
    while (abs(value) >= 1000 && unit < ByteUnits.lastIndex) {
        value /= 1000
        unit++
    }

    val number = if (unit == 0 || abs(value) >= 100) {
        value.roundToLong().toString()
    } else {
        val tenths = (value * 10).roundToLong()
        if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${abs(tenths % 10)}"
    }

    return "$number ${ByteUnits[unit]}"
}

internal fun formatBytesPerSecond(bytesPerSecond: Long): String = "${formatBytes(bytesPerSecond)}/s"

private val ByteUnits = listOf("B", "KB", "MB", "GB", "TB")
