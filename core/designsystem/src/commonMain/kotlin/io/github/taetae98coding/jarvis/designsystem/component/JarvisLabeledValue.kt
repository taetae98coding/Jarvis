package io.github.taetae98coding.jarvis.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/** 라벨은 왼쪽, 값은 오른쪽 끝에 붙는 한 줄. */
@Composable
fun JarvisLabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = JarvisLabeledValueDefaults.labelStyle,
    labelColor: Color = JarvisLabeledValueDefaults.labelColor,
    valueStyle: TextStyle = JarvisLabeledValueDefaults.valueStyle,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = labelStyle, color = labelColor)
        Text(text = value, style = valueStyle)
    }
}

object JarvisLabeledValueDefaults {
    val labelStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.bodyMedium

    val labelColor: Color
        @Composable @ReadOnlyComposable get() = JarvisTheme.colorScheme.onSurfaceVariant

    val valueStyle: TextStyle
        @Composable @ReadOnlyComposable get() = JarvisTheme.typography.bodyMedium
}
