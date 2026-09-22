package io.github.taetae98coding.jarvis.ui.app

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal const val TestFrameWidth = 100
internal const val TestFrameHeight = 200

// 100×200 짜리 단색 PNG. 제스처 좌표 기대값이 이 크기에서 나오므로, 프레임을 바꾸면 기대값도 바뀐다.
// 파일 대신 상수로 두는 이유는 Compose 리소스가 테스트마다 로딩 경로가 달라서다.
@OptIn(ExperimentalEncodingApi::class)
internal val TestFrame: ByteArray = Base64.decode(
    "iVBORw0KGgoAAAANSUhEUgAAAGQAAADICAIAAACRXtOWAAABJElEQVR42u3QAQ0AAAgDoEcykpGMbIUHYCM" +
        "BmT1KUSBLlixZsmQpkCVLlixZshTIkiVLlixZCmTJkiVLliwFsmTJkiVLlgJZsmTJkiVLgSxZsmTJkqVA" +
        "lixZsmTJUiBLlixZsmQpkCVLlixZshTIkiVLlixZCmTJkiVLliwFsmTJkiVLlgJZsmTJkiVLgSxZsmTJk" +
        "qVAlixZsmTJUiBLlixZsmQpkCVLlixZshTIkiVLlixZCmTJkiVLliwFsmTJkiVLlgJZsmTJkiVLgSxZsm" +
        "TJkqVAlixZsmTJUiBLlixZsmQpkCVLlixZshTIkiVLlixZCmTJkiVLliwFsmTJkiVLlgJZsmTJkiVLgSx" +
        "ZsmTJkqVAlixZsmTJUiBLlixZsmQp6D0TM+kaK9OWjAAAAABJRU5ErkJggg==",
)

internal val RunningAndroidDevice = EmulatorDevice(
    id = "emulator-5554",
    name = "Pixel_9_API_37",
    platform = EmulatorPlatform.ANDROID,
    isRunning = true,
    canControl = true,
)

internal val StoppedAndroidDevice = EmulatorDevice(
    id = "avd:Pixel_Tablet_API_36",
    name = "Pixel_Tablet_API_36",
    platform = EmulatorPlatform.ANDROID,
)

// 실행 중이어도 제스처는 받지 못한다. simctl 에 입력을 주입하는 명령이 없다.
internal val RunningSimulator = EmulatorDevice(
    id = "66C9B671-6289-44B1-9788-DF9528508CD7",
    name = "iPhone 17",
    platform = EmulatorPlatform.IOS,
    isRunning = true,
)
