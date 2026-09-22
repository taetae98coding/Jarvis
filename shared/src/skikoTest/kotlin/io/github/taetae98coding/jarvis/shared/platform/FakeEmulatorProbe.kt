package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeEmulatorProbe(initial: EmulatorStatus = EmulatorStatus()) : EmulatorProbe {
    val status = MutableStateFlow(initial)

    override fun observe(): Flow<EmulatorStatus> = status
}

// 기본값은 "셀 수 없음" 이다. 개수를 세지 못하는 타깃이 답하는 값과 같다.
internal fun fakeEmulatorProbe(
    android: EmulatorSummary? = null,
    ios: EmulatorSummary? = null,
): FakeEmulatorProbe = FakeEmulatorProbe(EmulatorStatus(android = android, ios = ios))
