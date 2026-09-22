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
    // 프로브가 답하기 전까지는 null 이다. 빈 상태로 시작하면 SDK 도구를 아직 실행하는 중인데도
    // "0개" 를 사실인 것처럼 보여주게 된다.
    val status by produceState<EmulatorStatus?>(initialValue = null, probe) {
        value = probe.probe()
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "에뮬레이터",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "이 머신에 설치된 Android 에뮬레이터와 iOS 시뮬레이터.",
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
            text = summary?.let { "실행 중 ${it.running}개 / 전체 ${it.total}개" } ?: "확인 중…",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
