package io.github.taetae98coding.jarvis.ui.terminal

import androidx.lifecycle.ViewModel
import io.github.taetae98coding.jarvis.domain.terminal.IsTerminalSupportedUseCase

internal class TerminalCardViewModel(
    isTerminalSupported: IsTerminalSupportedUseCase,
) : ViewModel() {
    val isSupported: Boolean = isTerminalSupported()
}
