package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorCard
import io.github.taetae98coding.jarvis.ui.emulator.EmulatorTestTag
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationCard
import io.github.taetae98coding.jarvis.ui.rotation.DeviceRotationTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.KeepSystemScreenAwakeTestTag
import io.github.taetae98coding.jarvis.ui.screen.ScreenAwakeCard
import io.github.taetae98coding.jarvis.ui.screen.SystemScreenAwakeCard

@Composable
internal fun FeatureGrid(
    state: JarvisAppState,
    modifier: Modifier = Modifier,
) {
    val keepScreenAwake by state.keepScreenAwake.collectAsState()
    val keepSystemScreenAwake by state.keepSystemScreenAwake.collectAsState()
    val systemScreenAwake by state.systemScreenAwake.collectAsState()
    val emulatorStatus by state.emulatorStatus.collectAsState()
    val deviceRotation by state.deviceRotation.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenAwakeCard(
                checked = keepScreenAwake,
                onCheckedChange = state::onKeepScreenAwakeChange,
                modifier = Modifier.testTag(KeepScreenAwakeTestTag),
            )
        }

        item {
            SystemScreenAwakeCard(
                status = systemScreenAwake,
                checked = keepSystemScreenAwake,
                onCheckedChange = state::onKeepSystemScreenAwakeChange,
                modifier = Modifier.testTag(KeepSystemScreenAwakeTestTag),
            )
        }

        item {
            EmulatorCard(
                status = emulatorStatus,
                modifier = Modifier.testTag(EmulatorTestTag),
            )
        }

        item {
            DeviceRotationCard(
                status = deviceRotation,
                onAngleClick = state::onDeviceRotationAngleClick,
                onRotate = state::onDeviceRotate,
                onLockedChange = state::onDeviceRotationLockChange,
                modifier = Modifier.testTag(DeviceRotationTestTag),
            )
        }
    }
}
