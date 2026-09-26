package io.github.taetae98coding.jarvis.ui.unitconverter

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

const val UnitConverterTestTag = "feature:unitconverter"
const val UnitConverterSummaryTestTag = "unitconverter:summary"

@Composable
fun UnitConverterCard(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<UnitConverterCardViewModel>()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    UnitConverterCard(
        summary = summary,
        onClick = { navigator.goTo(UnitConverterRoute) },
        modifier = modifier.testTag(UnitConverterTestTag),
    )
}

@Composable
internal fun UnitConverterCard(
    summary: UnitConverterSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = "단위 변환",
            icon = JarvisIcons.Ruler,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        if (summary == null) {
            Text(
                text = "길이·넓이(평)·무게(근·돈)·부피·온도·속도·데이터·시간·에너지. 눌러서 열고 값을 넣는다.",
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "${amountText(summary.input, summary.from)} = ${amountText(summary.result, summary.to)}",
                style = JarvisTheme.typography.titleSmall,
                modifier = Modifier.testTag(UnitConverterSummaryTestTag),
            )
        }
    }
}
