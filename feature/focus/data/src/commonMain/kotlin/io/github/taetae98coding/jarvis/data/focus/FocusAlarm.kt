package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.focus.FocusAlarmRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase

/** 무엇으로 알리는지는 각 플랫폼 스펙의 focus-timer 절에 있다. */
internal expect fun createFocusAlarm(context: PlatformContext): FocusAlarmRepository

internal class FocusAlarmMessage(
    val title: String,
    val body: String,
)

internal fun focusAlarmMessage(phase: FocusPhase): FocusAlarmMessage =
    when (phase) {
        FocusPhase.FOCUS -> FocusAlarmMessage("집중 끝", "잠깐 쉬어 갈 시간입니다.")
        FocusPhase.SHORT_BREAK, FocusPhase.LONG_BREAK -> FocusAlarmMessage("휴식 끝", "다시 집중할 시간입니다.")
    }
