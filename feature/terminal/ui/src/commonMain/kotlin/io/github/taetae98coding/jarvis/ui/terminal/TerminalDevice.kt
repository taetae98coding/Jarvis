package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButtonDefaults
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTabKind
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import io.github.taetae98coding.jarvis.ui.device.DeviceChoicePlatform
import io.github.taetae98coding.jarvis.ui.device.DeviceScreens

fun terminalDeviceTestTag(tabId: Long): String = "terminal:device:$tabId"

fun terminalDeviceLogToggleTestTag(tabId: Long): String = "terminal:device-log-toggle:$tabId"

/**
 * 기기 탭의 창. 기기 화면·제스처·로그 창은 [devices] 가 그리고, 여기서는 로그 버튼이 있는 도구 줄과 화면·로그를 나누는 배치,
 * 누르면 그룹 포커스를 주는 일만 한다(docs/common/device-logcat.html R1·R3).
 */
@Composable
internal fun TerminalDevice(
    tab: TerminalTab,
    devices: DeviceScreens?,
    onFocus: () -> Unit,
    onLogVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceId = tab.deviceId
    val currentOnFocus by rememberUpdatedState(onFocus)

    Column(
        modifier = modifier
            .testTag(terminalDeviceTestTag(tab.id))
            // 안쪽의 제스처가 먼저 소비하므로 Initial 단계에서 소비하지 않고 누름만 본다.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    currentOnFocus()
                }
            },
    ) {
        if (devices == null || deviceId == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "[기기 화면을 열 수 없습니다]",
                    style = JarvisTheme.typography.bodyMedium,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            JarvisIconButton(
                icon = JarvisIcons.Logs,
                contentDescription = if (tab.deviceLogVisible) "로그 숨기기" else "로그 보기",
                onClick = { onLogVisibleChange(!tab.deviceLogVisible) },
                colors = JarvisIconButtonDefaults.colors(
                    contentColor = if (tab.deviceLogVisible) JarvisTheme.colorScheme.primary else JarvisTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(terminalDeviceLogToggleTestTag(tab.id)),
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val screen = @Composable { screenModifier: Modifier -> devices.Screen(deviceId = deviceId, modifier = screenModifier) }
            val log = @Composable { logModifier: Modifier -> devices.Log(deviceId = deviceId, modifier = logModifier) }
            val spacing = JarvisTheme.dimens.spacing.xs

            when {
                !tab.deviceLogVisible -> screen(Modifier.fillMaxSize())

                maxWidth >= maxHeight -> Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    screen(Modifier.weight(1f).fillMaxHeight())
                    log(Modifier.weight(1f).fillMaxHeight())
                }

                else -> Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    screen(Modifier.weight(1f).fillMaxWidth())
                    log(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

internal val DeviceChoice.devicePlatform: DevicePlatform
    get() = when (platform) {
        DeviceChoicePlatform.Android -> DevicePlatform.Android
        DeviceChoicePlatform.IOS -> DevicePlatform.IOS
    }

internal val DeviceChoice.tabKind: TerminalTabKind
    get() = when (platform) {
        DeviceChoicePlatform.Android -> TerminalTabKind.Android
        DeviceChoicePlatform.IOS -> TerminalTabKind.IOS
    }
