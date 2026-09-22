package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.PlatformContext

// iOS 에는 자동 잠금 시간을 읽거나 쓰는 공개 API 가 없다. `idleTimerDisabled` 는 포그라운드 앱에만
// 주어지는 권한이고, 그 밖의 경로는 SpringBoard 권한이 필요해 앱스토어 배포를 포기해도 열리지 않는다.
internal actual fun createSystemScreenAwakeDataSource(context: PlatformContext): SystemScreenAwakeDataSource =
    UnsupportedSystemScreenAwakeDataSource
