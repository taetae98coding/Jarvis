package io.github.taetae98coding.jarvis.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.shared.settings.LocalAppSettings

@Composable
internal fun FeatureGrid(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ToggleFeatureCard(
                title = "Keep screen awake",
                description = "Stops the screen from turning off while Jarvis is in the foreground.",
                checked = settings.keepScreenAwake,
                onCheckedChange = { settings.keepScreenAwake = it },
                modifier = Modifier.testTag(KeepScreenAwakeTestTag),
            )
        }
    }
}

internal const val KeepScreenAwakeTestTag = "feature:keepScreenAwake"
