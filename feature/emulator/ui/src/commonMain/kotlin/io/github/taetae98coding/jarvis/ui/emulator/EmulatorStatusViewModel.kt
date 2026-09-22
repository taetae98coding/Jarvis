package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorStatusUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class EmulatorStatusViewModel(
    observeEmulatorStatus: ObserveEmulatorStatusUseCase,
) : ViewModel() {
    // 아직 답하지 않은 상태가 null 이다. 빈 상태로 시작하면 세는 중인데도 "0개" 를 사실인 것처럼
    // 보여주게 된다. Eagerly 라 카드가 "확인 중…" 에서 숫자로 한 번만 바뀐다.
    val status: StateFlow<EmulatorStatus?> =
        observeEmulatorStatus().stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
