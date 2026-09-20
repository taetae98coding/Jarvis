package io.github.taetae98coding.jarvis.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.shared.platform.KeepScreenAwake
import io.github.taetae98coding.jarvis.shared.platform.SettingsStore
import io.github.taetae98coding.jarvis.shared.platform.rememberSettingsStore
import io.github.taetae98coding.jarvis.shared.settings.AppSettings
import io.github.taetae98coding.jarvis.shared.settings.LocalAppSettings
import io.github.taetae98coding.jarvis.shared.ui.AppInfoCard
import io.github.taetae98coding.jarvis.shared.ui.FeatureGrid

@Composable
@Preview
fun App() {
    App(store = rememberSettingsStore())
}

@Composable
internal fun App(store: SettingsStore) {
    val settings = remember(store) { AppSettings(store) }

    MaterialTheme {
        CompositionLocalProvider(LocalAppSettings provides settings) {
            // Applied above the screen content so it outlives any single screen.
            KeepScreenAwake(settings.keepScreenAwake)

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AppInfoCard(modifier = Modifier.fillMaxWidth())

                    FeatureGrid(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
