package io.github.taetae98coding.jarvis.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 백스택에 키를 넣고 빼는 것이 이 앱의 화면 이동 전부다.
 *
 * 기능 모듈은 서로를 모르고 앱 셸도 기능의 라우트를 모른다. 그 사이를 잇는 유일한 값이 이것이라
 * `:core:ui` 에 둔다. 백스택 자체는 앱 셸의 컴포지션이 들고 있다(`rememberNavBackStack`).
 */
@Stable
class Navigator(
    private val backStack: NavBackStack<NavKey>,
) {
    fun goTo(key: NavKey) {
        backStack.add(key)
    }

    /** 첫 화면은 남긴다. 뒤로 갈 곳이 없는 화면에서 스택을 비우면 그릴 것이 없어진다. */
    fun back() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator 가 없다. JarvisApp() 안에서만 쓸 수 있다.")
}
