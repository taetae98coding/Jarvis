package io.github.taetae98coding.jarvis.ui.screen

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 홈 타일이 여는 기능 화면 둘. 앱 셸은 이 타입을 모르고, 등록은 [screenUiModule] 이 한다. */
@Serializable
internal sealed interface ScreenAwakeRoute : NavKey {
    @Serializable
    data object App : ScreenAwakeRoute

    @Serializable
    data object System : ScreenAwakeRoute
}
