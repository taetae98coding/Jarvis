package io.github.taetae98coding.jarvis.data.state

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration

/*
 * 플랫폼 상태를 Flow 로 노출할 때 쓰는 공용 규칙이다.
 *
 * 시스템이 변경을 알려주면 그 신호마다, 알려주지 않으면 일정 간격마다 값을 다시 읽는다. 어느
 * 쪽이든 첫 값은 구독 즉시 읽고 값이 그대로인 방출은 걸러내므로, 구독자는 상태가 콜백으로 오는지
 * 폴링으로 오는지 알 필요가 없다.
 *
 * 신호에 값을 담지 않고 다시 읽는 이유는, 플랫폼 콜백이 "무엇이 바뀌었는지" 를 알려주지 않는
 * 경우가 많아서다(예: iOS 의 NSUserDefaultsDidChangeNotification, Android 의 ContentObserver).
 */

/** 시스템이 변경을 알려줄 때 쓴다. [read] 가 느린 동안 쌓인 신호는 마지막 하나로 합쳐진다. */
internal fun <T> observeOnSignals(
    signals: Flow<Unit>,
    read: suspend () -> T,
): Flow<T> = signals.conflate().readEachTime(read)

/** 시스템이 변경을 알려주지 않을 때 쓴다. [interval] 마다 다시 읽는다. */
internal fun <T> observeByPolling(
    interval: Duration,
    read: suspend () -> T,
): Flow<T> =
    // 첫 읽기는 readEachTime 의 onStart 가 맡으므로 여기서는 간격만 센다.
    flow {
        while (true) {
            delay(interval)
            emit(Unit)
        }
    }.readEachTime(read)

/**
 * 콜백을 주는 플랫폼과 주지 않는 플랫폼이 섞여 있을 때 쓴다. [signals] 가 null 이면 [interval]
 * 폴링으로 대체한다.
 */
internal fun <T> observeSystemState(
    signals: Flow<Unit>?,
    interval: Duration,
    read: suspend () -> T,
): Flow<T> =
    if (signals == null) {
        observeByPolling(interval, read)
    } else {
        observeOnSignals(signals, read)
    }

private fun <T> Flow<Unit>.readEachTime(read: suspend () -> T): Flow<T> =
    onStart { emit(Unit) }
        .map { read() }
        .distinctUntilChanged()
