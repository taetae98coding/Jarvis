package io.github.taetae98coding.jarvis.ui.devtools

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

const val DevToolsTestTag = "feature:devtools"

@Composable
fun DevToolsCard(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current

    DevToolsCard(
        onClick = { navigator.goTo(DevToolsRoute) },
        modifier = modifier.testTag(DevToolsTestTag),
    )
}

@Composable
internal fun DevToolsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = DevToolsTitle,
            icon = JarvisIcons.Code,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        Text(
            text = "타임스탬프·Base64·URL·JSON·UUID·해시·색 변환. 눌러서 열고 결과를 복사한다.",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }
}
