package io.github.taetae98coding.jarvis.ui.terminal

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 셸은 백스택에 저장되지 않는다. 복원되면 새 셸 하나로 다시 시작한다. */
@Serializable
internal data object TerminalRoute : NavKey
