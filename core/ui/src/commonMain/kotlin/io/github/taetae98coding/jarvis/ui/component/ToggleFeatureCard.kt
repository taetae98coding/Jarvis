package io.github.taetae98coding.jarvis.ui.component

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardDefaults
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

@Composable
fun ToggleFeatureCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    JarvisCard(
        // toggleable 이 그리는 물결은 Card 안쪽 Surface 의 클립보다 바깥에서 그려져서, 그냥 두면
        // 카드의 둥근 모서리를 넘어 사각형으로 퍼진다. 같은 shape 로 미리 잘라 둔다. 카드의 기본
        // 그림자가 0dp 라 클립으로 잃는 것은 없다.
        modifier = modifier
            .clip(JarvisCardDefaults.shape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        JarvisCardHeader(
            title = title,
            icon = icon,
            enabled = enabled,
            // toggleable 시맨틱은 카드가 갖고 있으므로, 스위치는 표시 역할만 한다.
            trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        )

        Text(
            text = description,
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }
}
