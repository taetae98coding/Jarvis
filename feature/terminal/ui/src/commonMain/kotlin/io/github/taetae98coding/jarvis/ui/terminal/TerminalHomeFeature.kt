package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import org.koin.compose.viewmodel.koinViewModel

const val TerminalTitle = "터미널"

/** 타일은 카드가 눌렸을 때와 같은 터미널 화면을 연다. 기능 화면을 따로 두지 않는다. */
object TerminalHomeFeature : HomeFeature {
    override val id: String = "terminal"
    override val title: String = TerminalTitle
    override val icon: ImageVector = JarvisIcons.Terminal
    override val route: NavKey = TerminalRoute

    @Composable
    override fun isSupported(): Boolean = koinViewModel<TerminalCardViewModel>().isSupported

    @Composable
    override fun HomeCard(modifier: Modifier) {
        TerminalCard(modifier = modifier)
    }
}
