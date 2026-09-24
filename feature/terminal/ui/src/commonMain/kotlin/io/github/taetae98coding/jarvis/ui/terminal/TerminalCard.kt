package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

const val TerminalTestTag = "feature:terminal"

@Composable
fun TerminalCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<TerminalCardViewModel>()
    val navigator = LocalNavigator.current

    TerminalCard(
        isSupported = viewModel.isSupported,
        onClick = { navigator.goTo(TerminalRoute) },
        modifier = modifier.testTag(TerminalTestTag),
    )
}

@Composable
internal fun TerminalCard(
    isSupported: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, enabled = isSupported, modifier = modifier) {
        JarvisCardHeader(
            title = "터미널",
            icon = JarvisIcons.Terminal,
            enabled = isSupported,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        Text(
            text = if (isSupported) {
                "iTerm 처럼 셸을 띄운다. 창을 좌우·상하로 나누고 탭을 더할 수 있다."
            } else {
                "이 플랫폼에서는 셸을 실행할 수 없습니다."
            },
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }
}
