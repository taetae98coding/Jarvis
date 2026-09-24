package io.github.taetae98coding.jarvis.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeNotificationUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetSystemScreenAwakeNotificationPinnedUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationStatus
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 카드 둘이 같은 설정의 두 면이라 ViewModel 도 하나다. */
internal class ScreenAwakeViewModel(
    observeKeepScreenAwake: ObserveKeepScreenAwakeUseCase,
    observeKeepSystemScreenAwake: ObserveKeepSystemScreenAwakeUseCase,
    observeSystemScreenAwakeStatus: ObserveSystemScreenAwakeStatusUseCase,
    private val setKeepScreenAwake: SetKeepScreenAwakeUseCase,
    private val setKeepSystemScreenAwake: SetKeepSystemScreenAwakeUseCase,
    observeSystemScreenAwakeNotification: ObserveSystemScreenAwakeNotificationUseCase,
    private val setSystemScreenAwakeNotificationPinned: SetSystemScreenAwakeNotificationPinnedUseCase,
) : ViewModel() {
    val keepScreenAwake: StateFlow<Boolean> = observeKeepScreenAwake(viewModelScope)

    val keepSystemScreenAwake: StateFlow<Boolean> = observeKeepSystemScreenAwake(viewModelScope)

    val systemScreenAwake: StateFlow<SystemScreenAwakeStatus> = observeSystemScreenAwakeStatus(viewModelScope)

    val systemScreenAwakeNotification: StateFlow<SystemScreenAwakeNotificationStatus> =
        observeSystemScreenAwakeNotification(viewModelScope)

    fun onKeepScreenAwakeChange(value: Boolean) {
        setKeepScreenAwake(value)
    }

    fun onKeepSystemScreenAwakeChange(value: Boolean) {
        viewModelScope.launch { setKeepSystemScreenAwake(value) }
    }

    fun onSystemScreenAwakeNotificationPinnedChange(pinned: Boolean) {
        viewModelScope.launch { setSystemScreenAwakeNotificationPinned(pinned) }
    }
}
