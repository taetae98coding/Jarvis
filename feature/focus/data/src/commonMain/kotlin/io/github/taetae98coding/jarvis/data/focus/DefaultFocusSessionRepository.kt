package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import io.github.taetae98coding.jarvis.domain.focus.FocusSession
import io.github.taetae98coding.jarvis.domain.focus.FocusSessionRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusTimer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal class DefaultFocusSessionRepository(
    private val store: SettingsStore,
) : FocusSessionRepository {
    override fun observeFocusSession(): Flow<FocusSession> =
        store.observeString(FocusSessionKey, "").map(::decodeFocusSession)

    override fun readFocusSession(): FocusSession = decodeFocusSession(store.getString(FocusSessionKey, ""))

    override fun saveFocusSession(session: FocusSession) {
        store.putString(FocusSessionKey, encodeFocusSession(session))
    }

    internal companion object {
        const val FocusSessionKey = "focus_session"
    }
}

/*
 * SettingsStore 가 불리언과 문자열만 받아 한 줄로 적는다. 필드 하나씩 키를 나누면 두 번의 쓰기 사이에
 * 변경 신호가 끼어 반쯤 바뀐 상태를 읽을 수 있다.
 * 형식: 버전|단계|타이머 종류(i·r·p)|끝나는 시각(epoch ms)·남은 시간(ms)|주기 안 집중 수|오늘 수|오늘(epoch day)
 */
private const val FormatVersion = "1"
private const val Separator = '|'

internal fun encodeFocusSession(session: FocusSession): String {
    val (kind, value) = when (val timer = session.timer) {
        FocusTimer.Idle -> "i" to 0L
        is FocusTimer.Running -> "r" to timer.endsAt.toEpochMilliseconds()
        is FocusTimer.Paused -> "p" to timer.remaining.inWholeMilliseconds
    }

    return listOf(
        FormatVersion,
        session.phase.storedValue,
        kind,
        value.toString(),
        session.focusesInCycle.toString(),
        session.todayCount.toString(),
        session.todayEpochDay.toString(),
    ).joinToString(Separator.toString())
}

/** 비었거나 모르는 형식이면 처음 상태다. 앱 밖에서 잘못 쓴 값 때문에 카드가 죽지 않게 한다. */
internal fun decodeFocusSession(value: String): FocusSession {
    val parts = value.split(Separator)
    if (parts.size != 7 || parts[0] != FormatVersion) return FocusSession()

    val phase = FocusPhase.fromStored(parts[1]) ?: return FocusSession()
    val number = parts[3].toLongOrNull() ?: return FocusSession()
    val timer = when (parts[2]) {
        "i" -> FocusTimer.Idle
        "r" -> FocusTimer.Running(Instant.fromEpochMilliseconds(number))
        "p" -> FocusTimer.Paused(number.milliseconds)
        else -> return FocusSession()
    }

    return FocusSession(
        phase = phase,
        timer = timer,
        focusesInCycle = parts[4].toIntOrNull() ?: 0,
        todayCount = parts[5].toIntOrNull() ?: 0,
        todayEpochDay = parts[6].toLongOrNull() ?: 0,
    )
}
