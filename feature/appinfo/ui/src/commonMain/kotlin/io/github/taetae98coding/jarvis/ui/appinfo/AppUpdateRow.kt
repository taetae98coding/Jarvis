package io.github.taetae98coding.jarvis.ui.appinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValueDefaults
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/** 새 버전이 있을 때만 그린다(docs/common/app-update.html#behavior). */
@Composable
internal fun AppUpdateRow(
    state: AppUpdateUiState,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val version = when (state) {
        AppUpdateUiState.None -> return
        is AppUpdateUiState.Available -> state.version
        is AppUpdateUiState.Installing -> state.version
        is AppUpdateUiState.Failed -> state.version
    }

    Column(
        modifier = modifier.fillMaxWidth().testTag(AppUpdateTestTag),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = NewVersionLabel,
                style = JarvisLabeledValueDefaults.labelStyle,
                color = JarvisLabeledValueDefaults.labelColor,
            )
            Text(
                text = version,
                style = JarvisLabeledValueDefaults.valueStyle,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = onUpdateClick,
                enabled = state !is AppUpdateUiState.Installing,
                modifier = Modifier.testTag(AppUpdateButtonTestTag),
            ) {
                Text(
                    text = when (state) {
                        is AppUpdateUiState.Installing -> "설치 중…"
                        is AppUpdateUiState.Failed -> "다시 시도"
                        else -> "업데이트"
                    },
                )
            }
        }

        if (state is AppUpdateUiState.Failed) {
            Text(
                text = "업데이트하지 못했습니다: ${state.reason}",
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.error,
            )
        }
    }
}

const val NewVersionLabel = "새 버전"
const val AppUpdateTestTag = "appinfo:update"
const val AppUpdateButtonTestTag = "appinfo:update-button"
