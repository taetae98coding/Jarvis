package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.PlatformContext

// 디스플레이 회전을 바꾸는 공개 API 가 JDK 에도 macOS 에도 없다. CGDisplayRotation 은 읽기 전용이고,
// 쓰는 쪽은 IOKit 비공개 경로나 별도 설치가 필요한 CLI 뿐이다.
internal actual fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource =
    UnsupportedDeviceRotationDataSource
