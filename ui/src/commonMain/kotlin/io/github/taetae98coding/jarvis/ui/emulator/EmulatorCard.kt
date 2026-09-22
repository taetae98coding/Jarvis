package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary

internal const val EmulatorTestTag = "feature:emulator"

@Composable
internal fun EmulatorCard(
    status: EmulatorStatus?,
    modifier: Modifier = Modifier,
) {
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
                text = "개발자 머신의 Android 에뮬레이터와 iOS 시뮬레이터. 이 머신에서 셀 수 없는 " +
                    "타깃은 데스크탑 앱에 물어본다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SummaryRow(label = "Android", value = status.describe(EmulatorStatus::android))
            SummaryRow(label = "iOS", value = status.describe(EmulatorStatus::ios))
        }
    }
}

// 아직 세는 중 / 셀 방법이 없음 / 개수 세 가지를 구분한다. 뒤의 둘이 같아 보이면 SDK 도구를 못 찾은
// 것과 에뮬레이터가 없는 것을 읽는 사람이 가를 수 없다.
private fun EmulatorStatus?.describe(select: (EmulatorStatus) -> EmulatorSummary?): String {
    if (this == null) return "확인 중…"

    val summary = select(this) ?: return "셀 수 없음"

    return "실행 중 ${summary.running}개 / 전체 ${summary.total}개"
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
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
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
