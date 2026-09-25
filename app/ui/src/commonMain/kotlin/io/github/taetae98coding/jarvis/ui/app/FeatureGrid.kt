package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorCard
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationCard
import io.github.taetae98coding.jarvis.ui.screen.ScreenAwakeCard
import io.github.taetae98coding.jarvis.ui.screen.SystemScreenAwakeCard
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCard
import io.github.taetae98coding.jarvis.ui.theme.ThemeModeCard

/**
 * 기능이 늘 때 앱 셸에서 고치는 유일한 파일이다.
 *
 * 카드는 인자를 받지 않는다. 각 기능의 카드가 자기 ViewModel 을 Koin 에서 직접 받으므로 셸은
 * 그 기능의 유스케이스도 상태도 모른다.
 */
@Composable
internal fun FeatureGrid(modifier: Modifier = Modifier) {
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
        item { ScreenAwakeCard() }

        item { SystemScreenAwakeCard() }

        item { ThemeModeCard() }

        item { EmulatorCard() }

        item { DeviceRotationCard() }

        item { TerminalCard() }
    }
}
