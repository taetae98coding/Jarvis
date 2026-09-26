package io.github.taetae98coding.jarvis.ui.devtools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature

const val DevToolsTitle = "개발자 도구"

/** 공통 코드만으로 동작해 모든 플랫폼이 지원한다. 타일은 카드가 눌렸을 때와 같은 화면을 연다. */
object DevToolsHomeFeature : HomeFeature {
    override val id: String = "devTools"
    override val title: String = DevToolsTitle
    override val icon: ImageVector = JarvisIcons.Code
    override val route: NavKey = DevToolsRoute

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        DevToolsCard(modifier = modifier)
    }
}
