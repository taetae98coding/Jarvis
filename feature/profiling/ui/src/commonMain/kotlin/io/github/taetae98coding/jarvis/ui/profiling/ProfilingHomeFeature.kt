package io.github.taetae98coding.jarvis.ui.profiling

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.HomeFeatureScreen

const val ProfilingTitle = "프로파일링"

/**
 * 지표마다 지원이 갈리지만 어느 플랫폼에도 잴 수 있는 지표가 하나는 있어서 기능 자체는 늘 지원한다
 * (docs/common/profiling.html#platforms). 못 재는 지표는 카드가 줄마다 "측정 불가" 로 표시한다.
 */
object ProfilingHomeFeature : HomeFeature {
    override val id: String = "profiling"
    override val title: String = ProfilingTitle
    override val icon: ImageVector = JarvisIcons.Activity
    override val route: NavKey = ProfilingRoute

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        ProfilingCard(modifier = modifier)
    }
}

@Composable
internal fun ProfilingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = ProfilingTitle, onBack = onBack, modifier = modifier) {
        ProfilingCard(modifier = Modifier.fillMaxWidth())
    }
}
