package io.github.taetae98coding.jarvis.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.ui.component.ToggleFeatureCard

internal const val KeepScreenAwakeTestTag = "feature:keepScreenAwake"

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
