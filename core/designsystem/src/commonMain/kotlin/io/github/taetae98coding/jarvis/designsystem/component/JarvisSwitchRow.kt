package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/**
 * 카드 안의 "제목 + 스위치" 한 줄. 스위치 자체가 토글 시맨틱을 가지므로 카드가 통째로 토글일 때는 쓰지 않는다.
 *
 * [supporting] 은 스위치가 왜 효과가 없는지 같은 짧은 보조 문구다. null 이면 줄을 그리지 않는다.
 */
@Composable
fun JarvisSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supporting: String? = null,
    switchModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = JarvisTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = switchModifier,
                enabled = enabled,
            )
        }

        if (supporting != null) {
            Text(
                text = supporting,
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
