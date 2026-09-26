package io.github.taetae98coding.jarvis.ui.focus

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.HomeFeatureScreen

const val FocusTimerTitle = "집중 타이머"

/** 타이머는 공통 코드로 돌아 모든 플랫폼이 지원한다. 타일은 카드를 그대로 담은 기능 화면을 연다. */
object FocusTimerHomeFeature : HomeFeature {
    override val id: String = "focusTimer"
    override val title: String = FocusTimerTitle
    override val icon: ImageVector = JarvisIcons.Timer
    override val route: NavKey = FocusTimerRoute

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        FocusTimerCard(modifier = modifier)
    }
}

@Composable
internal fun FocusTimerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomeFeatureScreen(title = FocusTimerTitle, onBack = onBack, modifier = modifier) {
        FocusTimerCard(modifier = Modifier.fillMaxWidth())
    }
}
