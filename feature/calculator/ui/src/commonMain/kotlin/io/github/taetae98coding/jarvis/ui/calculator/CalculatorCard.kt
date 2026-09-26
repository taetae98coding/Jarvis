package io.github.taetae98coding.jarvis.ui.calculator

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

const val CalculatorTestTag = "feature:calculator"

@Composable
fun CalculatorCard(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current

    CalculatorCard(
        onClick = { navigator.goTo(CalculatorRoute) },
        modifier = modifier.testTag(CalculatorTestTag),
    )
}

@Composable
internal fun CalculatorCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = CalculatorTitle,
            icon = JarvisIcons.Calculator,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        Text(
            text = "수식 계산·퍼센트·BMI. 눌러서 열고 최근 계산을 다시 쓴다.",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )
    }
}
