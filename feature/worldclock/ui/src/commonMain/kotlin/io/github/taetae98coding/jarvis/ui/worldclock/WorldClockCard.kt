package io.github.taetae98coding.jarvis.ui.worldclock

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.worldclock.CityTime
import io.github.taetae98coding.jarvis.domain.worldclock.WorldClockState
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

const val WorldClockTestTag = "feature:worldclock"
const val WorldClockCardLocalTestTag = "$WorldClockTestTag:local"

fun worldClockCardCityTestTag(zoneId: String): String = "$WorldClockTestTag:city:$zoneId"

@Composable
fun WorldClockCard(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<WorldClockCardViewModel>()
    // 카드가 그리드에서 보이는 동안만 모은다. 스크롤해 나가거나 앱이 뒤로 가면 1초 틱도 멈춘다.
    val state by viewModel.state.collectAsStateWithLifecycle()

    WorldClockCard(
        state = state,
        onClick = { navigator.goTo(WorldClockRoute) },
        modifier = modifier.testTag(WorldClockTestTag),
    )
}

@Composable
internal fun WorldClockCard(
    state: WorldClockState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = "세계 시계",
            icon = JarvisIcons.Clock,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        Text(
            text = state.local.time.dateTime.clockText(withSeconds = true),
            modifier = Modifier.testTag(WorldClockCardLocalTestTag),
            style = JarvisTheme.typography.displaySmall,
        )

        Text(
            text = "${state.local.city.name} · ${state.local.time.dateTime.date.shortLabel()} · ${utcOffsetLabel(state.local.time.offsetSeconds)}",
            style = JarvisTheme.typography.bodySmall,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
        )

        state.cities.take(WorldClockCardCityLimit).forEach { CardCityRow(it) }
    }
}

@Composable
private fun CardCityRow(city: CityTime) {
    val day = dayDifferenceLabel(city.dayDifference)

    JarvisLabeledValue(
        label = city.city.name,
        value = listOfNotNull(day, city.time.dateTime.clockText(withSeconds = false)).joinToString(" "),
        modifier = Modifier.testTag(worldClockCardCityTestTag(city.city.zoneId)),
    )
}

/** 카드가 그리드에서 너무 길어지지 않게 앞의 몇 도시만 보인다. 나머지는 화면에 있다. */
internal const val WorldClockCardCityLimit = 3
