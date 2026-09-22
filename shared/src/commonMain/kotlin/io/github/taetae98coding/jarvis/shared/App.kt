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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.shared.platform.EmulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.PlatformIdleInhibitor
import io.github.taetae98coding.jarvis.shared.platform.SettingsStore
import io.github.taetae98coding.jarvis.shared.platform.emulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.keepScreenAwake
import io.github.taetae98coding.jarvis.shared.platform.rememberSettingsStore
import io.github.taetae98coding.jarvis.shared.platform.rememberSystemScreenAwake
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
internal fun App(
    store: SettingsStore,
    probe: EmulatorProbe = emulatorProbe,
) {
    val scope = rememberCoroutineScope()
    val settings = remember(store) { AppSettings(store, scope) }
    val keepScreenAwake by settings.keepScreenAwake.collectAsState()
    val keepSystemScreenAwake by settings.keepSystemScreenAwake.collectAsState()

    // 전역 화면 유지는 화면 수명보다 오래 사는 효과라 여기서 한 번만 걸고 상태를 아래로 내려보낸다.
    val systemScreenAwake = rememberSystemScreenAwake(keepSystemScreenAwake)

    MaterialTheme {
        CompositionLocalProvider(LocalAppSettings provides settings) {
            PlatformIdleInhibitor(keepScreenAwake)

            // 루트 Surface 에 붙여서 특정 화면의 수명과 무관하게 효과가 유지되도록 한다.
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .keepScreenAwake(keepScreenAwake),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AppInfoCard(modifier = Modifier.fillMaxWidth())

                    FeatureGrid(
                        emulatorProbe = probe,
                        systemScreenAwake = systemScreenAwake,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
