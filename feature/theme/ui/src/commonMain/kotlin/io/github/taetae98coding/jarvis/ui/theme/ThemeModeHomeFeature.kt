package io.github.taetae98coding.jarvis.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.HomeFeatureScreen

const val ThemeModeTitle = "화면 테마"

/** 테마는 Compose 가 그리므로 모든 플랫폼이 지원한다(docs/common/theme-mode.html#platforms). */
object ThemeModeHomeFeature : HomeFeature {
    override val id: String = "themeMode"
    override val title: String = ThemeModeTitle
    override val icon: ImageVector = JarvisIcons.Moon
    override val route: NavKey = ThemeModeRoute

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        ThemeModeCard(modifier = modifier)
    }
}

@Composable
internal fun ThemeModeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = ThemeModeTitle, onBack = onBack, modifier = modifier) {
        ThemeModeCard(modifier = Modifier.fillMaxWidth())
    }
}
