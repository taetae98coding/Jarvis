package io.github.taetae98coding.jarvis.data.screen

import io.github.taetae98coding.jarvis.data.PlatformContext

// 브라우저는 OS 의 전원 설정에 닿을 수 없다. Screen Wake Lock 은 페이지가 살아 있는 동안만 유효하다.
internal actual fun createSystemScreenAwakeDataSource(context: PlatformContext): SystemScreenAwakeDataSource =
    UnsupportedSystemScreenAwakeDataSource
