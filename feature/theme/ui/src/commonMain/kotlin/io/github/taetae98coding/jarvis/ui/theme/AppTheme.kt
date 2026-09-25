package io.github.taetae98coding.jarvis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.domain.theme.ApplyThemeModeUseCase
import io.github.taetae98coding.jarvis.domain.theme.ObserveThemeModeUseCase
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * 고른 테마 모드를 다크 여부로 풀어 준다. 앱 셸이 `JarvisTheme(darkTheme = appDarkTheme())` 한 줄로 쓴다.
 *
 * 앱 밖 표면에 모드를 알리는 효과도 여기서 시작된다. 어느 화면보다 오래 살아야 해서 앱 루트의
 * ViewModel 이 갖는다.
 */
@Composable
fun appDarkTheme(): Boolean {
    val viewModel = koinViewModel<ThemeEffectViewModel>()
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()

    return mode.isDark(isSystemInDarkTheme())
}

internal class ThemeEffectViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val applyThemeMode: ApplyThemeModeUseCase,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = observeThemeMode(viewModelScope)

    init {
        viewModelScope.launch { applyThemeMode() }
    }
}
