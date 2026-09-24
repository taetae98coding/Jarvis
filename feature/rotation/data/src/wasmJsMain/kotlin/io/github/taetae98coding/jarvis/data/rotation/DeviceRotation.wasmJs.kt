package io.github.taetae98coding.jarvis.data.rotation

import io.github.taetae98coding.jarvis.data.PlatformContext

// 브라우저 문서는 OS 의 자동 회전 설정에 닿을 수 없다. screen.orientation.lock() 은 탭만 돌리고
// 기기는 그대로라서 요구사항(공통 스펙 R9)이 아니다. 2026-09-22 에 그 우회를 넣었다가 걷어냈다.
// 경위는 docs/platform/web.html#device-rotation 에 있다.
internal actual fun createDeviceRotationDataSource(context: PlatformContext): DeviceRotationDataSource =
    UnsupportedDeviceRotationDataSource
