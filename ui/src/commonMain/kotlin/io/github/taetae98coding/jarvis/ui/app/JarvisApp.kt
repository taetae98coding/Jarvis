package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.ui.appinfo.AppInfoCard
import io.github.taetae98coding.jarvis.ui.screen.keepScreenAwake

@Composable
fun JarvisApp(
    state: JarvisAppState,
    modifier: Modifier = Modifier,
) {
    val keepScreenAwake by state.keepScreenAwake.collectAsState()

    MaterialTheme {
        // 카드보다 위에 둬서 카드가 스크롤 밖으로 나가도 효과가 유지되도록 한다.
        LaunchedEffect(state) { state.applyEffects() }

        Surface(
            modifier = modifier
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
                AppInfoCard(
                    appInfo = state.appInfo,
                    modifier = Modifier.fillMaxWidth(),
                )

                FeatureGrid(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
