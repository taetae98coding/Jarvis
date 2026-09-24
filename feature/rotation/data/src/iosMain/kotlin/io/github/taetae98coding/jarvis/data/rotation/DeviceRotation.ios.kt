package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.PlatformContext

// 제어 센터의 회전잠금을 읽거나 쓰는 공개 API 가 없다. requestGeometryUpdate 로 앱 창만 돌리는
// 길은 있지만 기기가 아니라 앱만 돌아서 요구사항(공통 스펙 R9)이 아니다. 2026-09-22 에 그 우회를
// 넣었다가 걷어냈다. 경위는 docs/platform/ios.html#device-rotation 에 있다.
internal actual fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource =
    UnsupportedDeviceRotationDataSource
