package io.github.taetae98coding.jarvis.ui.devtools

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 앱 셸은 이 타입을 모른다. 등록은 [devToolsUiModule] 이 한다. */
@Serializable
internal data object DevToolsRoute : NavKey
