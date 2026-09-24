package io.github.taetae98coding.jarvis.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.screen.ApplyKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ApplySystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 앱 수명 효과. 어느 화면보다 오래 살아야 해서 화면이 아니라 앱 루트의 ViewModel 이 갖는다.
 */
internal class ScreenAwakeEffectViewModel(
    observeKeepScreenAwake: ObserveKeepScreenAwakeUseCase,
    private val applyKeepScreenAwake: ApplyKeepScreenAwakeUseCase,
    private val applySystemScreenAwake: ApplySystemScreenAwakeUseCase,
) : ViewModel() {
    val keepScreenAwake: StateFlow<Boolean> = observeKeepScreenAwake(viewModelScope)

    init {
        // 화면에서 LaunchedEffect 로 걸면 화면이 바뀔 때마다 효과가 끊기고 다시 걸린다. 취소될 때
        // 무엇을 되돌리는지는 각 유스케이스가 정한다.
        viewModelScope.launch { applyKeepScreenAwake() }
        viewModelScope.launch { applySystemScreenAwake() }
    }
}
