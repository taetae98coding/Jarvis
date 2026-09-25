package io.github.taetae98coding.jarvis.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.theme.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

const val ThemeModeTestTag = "feature:themeMode"

fun themeModeOptionTestTag(mode: ThemeMode): String = "$ThemeModeTestTag:${mode.storedValue}"

@Composable
fun ThemeModeCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ThemeModeViewModel>()
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()

    ThemeModeCard(
        mode = mode,
        onModeChange = viewModel::onThemeModeChange,
        modifier = modifier.testTag(ThemeModeTestTag),
    )
}

@Composable
internal fun ThemeModeCard(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(modifier = modifier) {
        JarvisCardHeader(title = "화면 테마", icon = JarvisIcons.Moon)

        Text(
            text = "앱의 밝은 색과 어두운 색을 고릅니다. 시스템 설정은 OS 의 다크 모드를 따릅니다.",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )

        Column(modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { option ->
                ThemeModeOption(
                    option = option,
                    selected = option == mode,
                    onClick = { onModeChange(option) },
                )
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    option: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(themeModeOptionTestTag(option))
            .padding(vertical = JarvisTheme.dimens.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // selectable 시맨틱은 줄이 갖고 있으므로, 라디오 버튼은 표시 역할만 한다.
        RadioButton(selected = selected, onClick = null)

        Text(text = option.label, style = JarvisTheme.typography.bodyMedium)
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "시스템 설정"
        ThemeMode.LIGHT -> "라이트"
        ThemeMode.DARK -> "다크"
    }
