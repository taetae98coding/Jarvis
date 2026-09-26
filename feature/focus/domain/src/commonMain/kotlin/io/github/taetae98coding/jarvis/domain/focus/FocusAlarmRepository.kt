package io.github.taetae98coding.jarvis.domain.focus

import kotlin.time.Instant

/**
 * 단계가 끝나는 시각에 앱 밖으로 알린다. 무엇으로 알리는지와 앱이 꺼져도 되는지는 플랫폼마다 다르다
 * (docs/common/focus-timer.html#platforms).
 */
interface FocusAlarmRepository {
    /** 걸려 있던 알림은 새것으로 바뀐다. 알림 권한이 필요하면 여기서 요청한다. */
    fun schedule(at: Instant, phase: FocusPhase)

    fun cancel()
}
