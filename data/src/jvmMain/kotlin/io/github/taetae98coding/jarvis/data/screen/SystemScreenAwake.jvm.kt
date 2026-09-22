package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.PlatformContext

// macOS 에서 시스템 유휴 시간을 바꾸는 건 `pmset` 이고 root 권한이 필요하다. `caffeinate` 는 프로세스가
// 살아 있는 동안만 유효해서 앱이 종료된 뒤를 커버하지 못한다. 그래서 데스크탑은 지원하지 않는다.
internal actual fun createSystemScreenAwakeDataSource(context: PlatformContext): SystemScreenAwakeDataSource =
    UnsupportedSystemScreenAwakeDataSource
