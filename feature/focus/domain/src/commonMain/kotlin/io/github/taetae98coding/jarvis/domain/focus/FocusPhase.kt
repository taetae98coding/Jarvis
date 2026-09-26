package io.github.taetae98coding.jarvis.domain.focus

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class FocusPhase(val duration: Duration) {
    FOCUS(25.minutes),
    SHORT_BREAK(5.minutes),
    LONG_BREAK(15.minutes),
    ;

    /** 저장소에 적는 문자열. 서수 대신 이름을 써서 상수 순서를 바꿔도 저장값이 뒤바뀌지 않는다. */
    val storedValue: String get() = name.lowercase()

    companion object {
        /** 긴 휴식은 집중을 이만큼 마칠 때마다 온다. */
        const val FocusesPerLongBreak = 4

        fun fromStored(value: String?): FocusPhase? = entries.firstOrNull { it.storedValue == value }
    }
}
