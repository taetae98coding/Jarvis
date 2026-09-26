package io.github.taetae98coding.jarvis.ui.focus

import kotlin.time.Duration

/** 분은 60 을 넘어도 그대로 적는다. 단계가 가장 길어야 25분이라 시간 자리가 필요 없다. */
internal fun formatRemaining(remaining: Duration): String {
    val seconds = remaining.inWholeSeconds.coerceAtLeast(0)
    return "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
}
