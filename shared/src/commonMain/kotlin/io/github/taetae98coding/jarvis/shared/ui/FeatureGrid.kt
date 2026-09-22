package io.github.taetae98coding.jarvis.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.shared.platform.EmulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.SystemScreenAwakeState
import io.github.taetae98coding.jarvis.shared.settings.LocalAppSettings

@Composable
internal fun FeatureGrid(
    emulatorProbe: EmulatorProbe,
    systemScreenAwake: SystemScreenAwakeState,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val keepScreenAwake by settings.keepScreenAwake.collectAsState()
    val keepSystemScreenAwake by settings.keepSystemScreenAwake.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ToggleFeatureCard(
                title = "화면 꺼짐 방지",
                description = "Jarvis 가 화면에 떠 있는 동안 화면이 꺼지지 않게 합니다.",
                checked = keepScreenAwake,
                onCheckedChange = settings::setKeepScreenAwake,
                modifier = Modifier.testTag(KeepScreenAwakeTestTag),
            )
        }

        item {
            SystemScreenAwakeCard(
                state = systemScreenAwake,
                checked = keepSystemScreenAwake,
                onCheckedChange = settings::setKeepSystemScreenAwake,
                modifier = Modifier.testTag(KeepSystemScreenAwakeTestTag),
            )
        }

        item {
            EmulatorCard(
                probe = emulatorProbe,
                modifier = Modifier.testTag(EmulatorTestTag),
            )
        }
    }
}

internal const val KeepScreenAwakeTestTag = "feature:keepScreenAwake"
internal const val KeepSystemScreenAwakeTestTag = "feature:keepSystemScreenAwake"
internal const val EmulatorTestTag = "feature:emulator"
