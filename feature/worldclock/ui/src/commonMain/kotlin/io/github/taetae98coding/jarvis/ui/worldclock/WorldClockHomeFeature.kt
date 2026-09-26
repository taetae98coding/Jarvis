package io.github.taetae98coding.jarvis.ui.worldclock

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature

const val WorldClockTitle = "세계 시계"

/** 공통 코드만으로 동작해 모든 플랫폼이 지원한다. 타일은 카드가 눌렸을 때와 같은 화면을 연다. */
object WorldClockHomeFeature : HomeFeature {
    override val id: String = "worldClock"
    override val title: String = WorldClockTitle
    override val icon: ImageVector = JarvisIcons.Clock
    override val route: NavKey = WorldClockRoute

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        WorldClockCard(modifier = modifier)
    }
}
