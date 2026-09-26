package io.github.taetae98coding.jarvis.ui.texttools

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

const val TextToolsTestTag = "feature:texttools"

@Composable
fun TextToolsCard(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current

    TextToolsCard(
        onClick = { navigator.goTo(TextToolsRoute) },
        modifier = modifier.testTag(TextToolsTestTag),
    )
}

@Composable
internal fun TextToolsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = TextToolsTitle,
            icon = JarvisIcons.Type,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        Text(
            text = "글자 수 세기·비밀번호 생성·대소문자 변환. 눌러서 열고 결과를 복사한다.",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }
}
