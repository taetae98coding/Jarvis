package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.ui.appinfo.AppInfoCard

@Composable
internal fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppInfoCard(modifier = Modifier.fillMaxWidth())

        FeatureGrid(modifier = Modifier.fillMaxWidth())
    }
}
