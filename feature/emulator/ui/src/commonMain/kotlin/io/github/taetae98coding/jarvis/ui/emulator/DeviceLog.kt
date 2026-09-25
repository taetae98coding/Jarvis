package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.emulator.DeviceLogLevel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

const val DeviceLogTestTag = "device:log"
const val DeviceLogFilterTestTag = "device:log-filter"
const val DeviceLogClearTestTag = "device:log-clear"
const val DeviceLogLineTestTag = "device:log-line"

/** 기기 탭의 로그 창(docs/common/device-logcat.html R4–R10). 컴포지션에 있는 동안에만 로그를 읽는다. */
@Composable
internal fun DeviceLog(
    viewModel: DeviceLogViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .testTag(DeviceLogTestTag)
            .background(JarvisTheme.colorScheme.surfaceContainerLow, JarvisTheme.shapes.small)
            .padding(JarvisTheme.dimens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
        ) {
            FilterField(value = filter, onValueChange = viewModel::setFilter, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::clear, modifier = Modifier.testTag(DeviceLogClearTestTag)) {
                Text(text = "지우기")
            }
        }

        val message = when {
            !state.isSupported -> "이 기기의 로그는 볼 수 없습니다."
            !state.hasReceived -> "로그를 기다리는 중…"
            state.lines.isEmpty() -> "필터에 맞는 줄이 없습니다"
            else -> null
        }

        if (message != null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = message,
                    style = JarvisTheme.typography.bodyMedium,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LogLines(lines = state.lines, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun LogLines(
    lines: List<DeviceLogEntry>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var follow by remember { mutableStateOf(true) }

    // 스크롤이 끝날 때마다 맨 아래인지로 따라가기를 정한다(R6). 아래의 scrollToItem 도 스크롤이라 끝나면 맨 아래라 따라가기가 유지된다.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { inProgress -> !inProgress }
            .collect { follow = !listState.canScrollForward }
    }

    val lastId = lines.lastOrNull()?.id
    LaunchedEffect(lastId, lines.size) {
        if (follow && !listState.isScrollInProgress && lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    val colors = LogLevelColors()

    SelectionContainer(modifier = modifier) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(lines, key = DeviceLogEntry::id) { entry ->
                Text(
                    text = entry.line.text,
                    style = JarvisTheme.codeTextStyle,
                    color = colors.of(entry.line.level),
                    modifier = Modifier.fillMaxWidth().testTag(DeviceLogLineTestTag),
                )
            }
        }
    }
}

@Composable
private fun FilterField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = JarvisTheme.colorScheme.onSurface
    val textStyle = JarvisTheme.typography.bodySmall.copy(color = textColor)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(textColor),
        modifier = modifier
            .testTag(DeviceLogFilterTestTag)
            .background(JarvisTheme.colorScheme.surfaceVariant, JarvisTheme.shapes.small)
            .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) {
                    Text(text = "필터", style = textStyle, color = JarvisTheme.colorScheme.onSurfaceVariant)
                }
                field()
            }
        },
    )
}

private class LogLevelColors(
    private val quiet: Color,
    private val normal: Color,
    private val warning: Color,
    private val error: Color,
) {
    fun of(level: DeviceLogLevel): Color =
        when (level) {
            DeviceLogLevel.Verbose, DeviceLogLevel.Debug -> quiet
            DeviceLogLevel.Info, DeviceLogLevel.Unknown -> normal
            DeviceLogLevel.Warn -> warning
            DeviceLogLevel.Error, DeviceLogLevel.Fatal -> error
        }
}

@Composable
private fun LogLevelColors(): LogLevelColors =
    LogLevelColors(
        quiet = JarvisTheme.colorScheme.onSurfaceVariant,
        normal = JarvisTheme.colorScheme.onSurface,
        warning = JarvisTheme.colors.warning,
        error = JarvisTheme.colorScheme.error,
    )
