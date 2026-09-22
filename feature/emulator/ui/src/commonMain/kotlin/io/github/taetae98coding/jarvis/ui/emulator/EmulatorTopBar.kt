package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Material3 의 TopAppBar 는 실험 API 라 버전을 올릴 때마다 시그니처가 흔들린다. 뒤로 버튼과 제목만
// 필요해서 직접 놓는다.
@Composable
internal fun EmulatorTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text(text = "← 뒤로") }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
