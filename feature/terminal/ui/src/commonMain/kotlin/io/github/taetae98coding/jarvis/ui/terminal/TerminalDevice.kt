package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTabKind
import io.github.taetae98coding.jarvis.ui.device.DeviceChoice
import io.github.taetae98coding.jarvis.ui.device.DeviceChoicePlatform
import io.github.taetae98coding.jarvis.ui.device.DeviceScreens

fun terminalDeviceTestTag(tabId: Long): String = "terminal:device:$tabId"

/** 기기 탭의 창. 기기 화면과 제스처는 [devices] 가 그리고, 여기서는 누르면 그룹 포커스만 준다. */
@Composable
internal fun TerminalDevice(
    tab: TerminalTab,
    devices: DeviceScreens?,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceId = tab.deviceId
    val currentOnFocus by rememberUpdatedState(onFocus)

    Box(
        modifier = modifier
            .testTag(terminalDeviceTestTag(tab.id))
            // 안쪽의 제스처가 먼저 소비하므로 Initial 단계에서 소비하지 않고 누름만 본다.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    currentOnFocus()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (devices == null || deviceId == null) {
            Text(
                text = "[기기 화면을 열 수 없습니다]",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            devices.Screen(deviceId = deviceId, modifier = Modifier.fillMaxSize())
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
