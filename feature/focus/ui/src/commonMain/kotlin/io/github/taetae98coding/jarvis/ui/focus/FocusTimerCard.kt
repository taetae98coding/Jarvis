package io.github.taetae98coding.jarvis.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import io.github.taetae98coding.jarvis.domain.focus.FocusStatus
import io.github.taetae98coding.jarvis.domain.focus.FocusTimerState
import org.koin.compose.viewmodel.koinViewModel

const val FocusTimerTestTag = "feature:focusTimer"
const val FocusTimerPhaseTestTag = "$FocusTimerTestTag:phase"
const val FocusTimerRemainingTestTag = "$FocusTimerTestTag:remaining"
const val FocusTimerPrimaryTestTag = "$FocusTimerTestTag:primary"
const val FocusTimerSkipTestTag = "$FocusTimerTestTag:skip"
const val FocusTimerResetTestTag = "$FocusTimerTestTag:reset"
const val FocusTimerTodayTestTag = "$FocusTimerTestTag:today"

@Composable
fun FocusTimerCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<FocusTimerViewModel>()
    val timer by viewModel.timer.collectAsStateWithLifecycle()

    FocusTimerCard(
        timer = timer,
        onStart = viewModel::onStart,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onSkip = viewModel::onSkip,
        onReset = viewModel::onReset,
        modifier = modifier.testTag(FocusTimerTestTag),
    )
}

@Composable
internal fun FocusTimerCard(
    timer: FocusTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(modifier = modifier) {
        JarvisCardHeader(title = "집중 타이머", icon = JarvisIcons.Timer)

        Text(
            text = timer.phase.label,
            modifier = Modifier.testTag(FocusTimerPhaseTestTag),
            style = JarvisTheme.typography.titleSmall,
            color = JarvisTheme.colorScheme.primary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatRemaining(timer.remaining),
                modifier = Modifier.testTag(FocusTimerRemainingTestTag),
                style = JarvisTheme.typography.displaySmall,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                when (timer.status) {
                    FocusStatus.IDLE -> PrimaryButton(JarvisIcons.Play, "시작", onStart)
                    FocusStatus.RUNNING -> PrimaryButton(JarvisIcons.Pause, "일시정지", onPause)
                    FocusStatus.PAUSED -> PrimaryButton(JarvisIcons.Play, "재개", onResume)
                }

                JarvisIconButton(
                    icon = JarvisIcons.SkipForward,
                    contentDescription = "건너뛰기",
                    onClick = onSkip,
                    modifier = Modifier.testTag(FocusTimerSkipTestTag),
                )

                JarvisIconButton(
                    icon = JarvisIcons.RotateLeft,
                    contentDescription = "초기화",
                    onClick = onReset,
                    modifier = Modifier.testTag(FocusTimerResetTestTag),
                )
            }
        }

        LinearProgressIndicator(
            progress = { timer.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        JarvisLabeledValue(
            label = "오늘 마친 집중",
            value = "${timer.todayCount}회",
            modifier = Modifier
                .testTag(FocusTimerTodayTestTag)
                .semantics(mergeDescendants = true) {},
        )

        // 긴 휴식까지 몇 번 남았는지. 긴 휴식 대기 중이면 4 / 4 다.
        JarvisLabeledValue(
            label = "긴 휴식까지",
            value = "${timer.focusesInCycle.coerceAtMost(FocusPhase.FocusesPerLongBreak)} / ${FocusPhase.FocusesPerLongBreak}",
        )
    }
}

@Composable
private fun PrimaryButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    JarvisIconButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        modifier = Modifier.testTag(FocusTimerPrimaryTestTag),
    )
}

internal val FocusPhase.label: String
    get() = when (this) {
        FocusPhase.FOCUS -> "집중"
        FocusPhase.SHORT_BREAK -> "짧은 휴식"
        FocusPhase.LONG_BREAK -> "긴 휴식"
    }
