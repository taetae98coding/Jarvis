package io.github.taetae98coding.jarvis.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.shared.platform.EmulatorProbe
import io.github.taetae98coding.jarvis.shared.platform.EmulatorStatus
import io.github.taetae98coding.jarvis.shared.platform.EmulatorSummary

@Composable
internal fun EmulatorCard(
    probe: EmulatorProbe,
    modifier: Modifier = Modifier,
) {
    // Null until the probe answers. Starting from an empty status instead would state "0 devices"
    // as a fact while the SDK tools are still being shelled out to.
    val status by produceState<EmulatorStatus?>(initialValue = null, probe) {
        value = probe.probe()
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Emulator",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "Android emulators and iOS simulators installed on this machine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SummaryRow(label = "Android", summary = status?.android)
            SummaryRow(label = "iOS", summary = status?.ios)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    summary: EmulatorSummary?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = summary?.let { "${it.running} running / ${it.total} total" } ?: "Checking…",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
