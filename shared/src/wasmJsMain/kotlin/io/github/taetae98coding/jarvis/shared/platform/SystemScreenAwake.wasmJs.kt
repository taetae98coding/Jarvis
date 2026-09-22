package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable

// 브라우저는 OS 의 전원 설정에 닿을 수 없다. Screen Wake Lock 은 페이지가 살아 있는 동안만 유효하다.
@Composable
internal actual fun rememberSystemScreenAwake(enabled: Boolean): SystemScreenAwakeState =
    UnsupportedSystemScreenAwake
