package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    Card(onClick = onClick, enabled = isSupported, modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "터미널",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = if (isSupported) {
                    "iTerm 처럼 셸을 띄운다. 패널을 좌우·상하로 나누고 탭을 더할 수 있다."
                } else {
                    "이 플랫폼에서는 셸을 실행할 수 없습니다."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
