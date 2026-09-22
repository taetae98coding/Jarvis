package io.github.taetae98coding.jarvis.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.ui.component.ToggleFeatureCard
import org.koin.compose.viewmodel.koinViewModel

const val KeepScreenAwakeTestTag = "feature:keepScreenAwake"

@Composable
fun ScreenAwakeCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ScreenAwakeViewModel>()
    val checked by viewModel.keepScreenAwake.collectAsState()

    ScreenAwakeCard(
        checked = checked,
        onCheckedChange = viewModel::onKeepScreenAwakeChange,
        modifier = modifier.testTag(KeepScreenAwakeTestTag),
    )
}

@Composable
internal fun ScreenAwakeCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleFeatureCard(
        title = "화면 꺼짐 방지",
        description = "Jarvis 가 화면에 떠 있는 동안 화면이 꺼지지 않게 합니다.",
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    )
}
