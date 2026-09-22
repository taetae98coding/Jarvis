package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorCard
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationCard
import io.github.taetae98coding.jarvis.ui.screen.ScreenAwakeCard
import io.github.taetae98coding.jarvis.ui.screen.SystemScreenAwakeCard

/**
 * 기능이 늘 때 앱 셸에서 고치는 유일한 파일이다.
 *
 * 카드는 인자를 받지 않는다. 각 기능의 카드가 자기 ViewModel 을 Koin 에서 직접 받으므로 셸은
 * 그 기능의 유스케이스도 상태도 모른다.
 */
@Composable
internal fun FeatureGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenAwakeCard() }

        item { SystemScreenAwakeCard() }

        item { EmulatorCard() }

        item { DeviceRotationCard() }
    }
}
