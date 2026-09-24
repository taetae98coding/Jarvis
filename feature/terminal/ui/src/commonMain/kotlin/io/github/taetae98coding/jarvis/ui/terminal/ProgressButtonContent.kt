package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

// 글자를 지우지 않고 투명하게 남겨서, 진행 중에도 버튼 크기와 옆 버튼 위치가 그대로다.
@Composable
internal fun ProgressButtonContent(text: String, inProgress: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.alpha(if (inProgress) 0f else 1f))
        if (inProgress) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
            )
        }
    }
}
