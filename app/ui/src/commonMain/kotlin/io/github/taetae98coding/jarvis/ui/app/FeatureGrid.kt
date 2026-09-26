package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTile
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.ui.battery.BatteryHomeFeature
import io.github.taetae98coding.jarvis.ui.calculator.CalculatorHomeFeature
import io.github.taetae98coding.jarvis.ui.devtools.DevToolsHomeFeature
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorHomeFeature
import io.github.taetae98coding.jarvis.ui.focus.FocusTimerHomeFeature
import io.github.taetae98coding.jarvis.ui.home.HomeFeature
import io.github.taetae98coding.jarvis.ui.home.homeFeatureTileTestTag
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.profiling.ProfilingHomeFeature
import io.github.taetae98coding.jarvis.ui.qrcode.QrCodeHomeFeature
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationHomeFeature
import io.github.taetae98coding.jarvis.ui.screen.ScreenAwakeHomeFeature
import io.github.taetae98coding.jarvis.ui.screen.SystemScreenAwakeHomeFeature
import io.github.taetae98coding.jarvis.ui.terminal.TerminalHomeFeature
import io.github.taetae98coding.jarvis.ui.texttools.TextToolsHomeFeature
import io.github.taetae98coding.jarvis.ui.theme.ThemeModeHomeFeature
import io.github.taetae98coding.jarvis.ui.unitconverter.UnitConverterHomeFeature
import io.github.taetae98coding.jarvis.ui.worldclock.WorldClockHomeFeature

/**
 * 기능이 늘 때 앱 셸에서 고치는 유일한 파일이다. 여기 적은 순서가 지원 여부가 같은 기능들 사이의 순서다.
 *
 * 셸은 기능의 [HomeFeature] 만 본다. 지원 여부와 카드는 기능이 자기 ViewModel 을 Koin 에서 직접 받아 내놓으므로
 * 셸은 그 기능의 유스케이스도 상태도 모른다(docs/common/home-adaptive-layout.html).
 */
private val HomeFeatures: List<HomeFeature> = listOf(
    ScreenAwakeHomeFeature,
    SystemScreenAwakeHomeFeature,
    ThemeModeHomeFeature,
    ProfilingHomeFeature,
    EmulatorHomeFeature,
    DeviceRotationHomeFeature,
    TerminalHomeFeature,
    BatteryHomeFeature,
    FocusTimerHomeFeature,
    DevToolsHomeFeature,
    UnitConverterHomeFeature,
    TextToolsHomeFeature,
    CalculatorHomeFeature,
    QrCodeHomeFeature,
    WorldClockHomeFeature,
)

@Composable
internal fun FeatureGrid(modifier: Modifier = Modifier) {
    val entries = rememberOrderedFeatures()

    when (currentWindowWidthClass()) {
        WindowWidthClass.Compact -> FeatureTileGrid(entries, modifier)
        WindowWidthClass.Medium -> FeatureCardGrid(entries, modifier)
    }
}

private class FeatureEntry(val feature: HomeFeature, val supported: Boolean)

/** 지원하는 기능이 앞, 나머지는 [HomeFeatures] 의 순서 그대로다(안정 정렬). */
@Composable
private fun rememberOrderedFeatures(): List<FeatureEntry> {
    val supported = HomeFeatures.map { it.isSupported() }

    return remember(supported) {
        HomeFeatures.mapIndexed { index, feature -> FeatureEntry(feature, supported[index]) }
            .sortedByDescending { it.supported }
    }
}

@Composable
private fun FeatureCardGrid(entries: List<FeatureEntry>, modifier: Modifier = Modifier) {
    // 카드 높이가 내용에 따라 제각각이라 staggered 를 쓴다. 행 단위로 묶는 LazyVerticalGrid 에서는
    // 한 줄의 카드가 모두 가장 긴 카드의 높이로 늘어나 짧은 카드 아래에 빈 공간이 남는다.
    val dimens = JarvisTheme.dimens

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = dimens.layout.gridMinCellWidth),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacing.m),
        // 열마다 항목 수가 달라 세로는 Arrangement 대신 항목 간격으로 준다.
        verticalItemSpacing = dimens.spacing.m,
    ) {
        items(entries, key = { it.feature.id }) { entry ->
            entry.feature.HomeCard(Modifier)
        }
    }
}

@Composable
private fun FeatureTileGrid(entries: List<FeatureEntry>, modifier: Modifier = Modifier) {
    val dimens = JarvisTheme.dimens
    val navigator = LocalNavigator.current

    // 타일은 높이가 모두 같아서 행으로 묶어도 빈 공간이 생기지 않는다.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = dimens.layout.gridMinTileWidth),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacing.m),
        verticalArrangement = Arrangement.spacedBy(dimens.spacing.m),
    ) {
        items(entries, key = { it.feature.id }) { entry ->
            val feature = entry.feature

            JarvisTile(
                icon = feature.icon,
                label = feature.title,
                onClick = { navigator.goTo(feature.route) },
                modifier = Modifier.testTag(homeFeatureTileTestTag(feature)),
                // 지원하지 않는 기능의 카드가 잠기는 것과 같다. 열어도 할 수 있는 것이 없다.
                enabled = entry.supported,
            )
        }
    }
}
