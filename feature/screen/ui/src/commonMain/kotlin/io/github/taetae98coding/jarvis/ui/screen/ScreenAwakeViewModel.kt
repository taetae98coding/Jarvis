package io.github.taetae98coding.jarvis.ui.screen

import androidx.lifecycle.ViewModel
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.ObserveSystemScreenAwakeStatusUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import kotlinx.coroutines.flow.StateFlow

/** 카드 둘이 같은 설정의 두 면이라 ViewModel 도 하나다. */
internal class ScreenAwakeViewModel(
    observeKeepScreenAwake: ObserveKeepScreenAwakeUseCase,
    observeKeepSystemScreenAwake: ObserveKeepSystemScreenAwakeUseCase,
    observeSystemScreenAwakeStatus: ObserveSystemScreenAwakeStatusUseCase,
    private val setKeepScreenAwake: SetKeepScreenAwakeUseCase,
    private val setKeepSystemScreenAwake: SetKeepSystemScreenAwakeUseCase,
) : ViewModel() {
    val keepScreenAwake: StateFlow<Boolean> = observeKeepScreenAwake()

    val keepSystemScreenAwake: StateFlow<Boolean> = observeKeepSystemScreenAwake()

    val systemScreenAwake: StateFlow<SystemScreenAwakeStatus> = observeSystemScreenAwakeStatus()

    fun onKeepScreenAwakeChange(value: Boolean) {
        setKeepScreenAwake(value)
    }

    fun onKeepSystemScreenAwakeChange(value: Boolean) {
        setKeepSystemScreenAwake(value)
    }
}
