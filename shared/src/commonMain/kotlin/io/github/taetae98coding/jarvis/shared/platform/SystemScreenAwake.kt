package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlin.time.Duration

/**
 * 앱이 화면에 없을 때도 화면을 켜 두는 기능의 상태.
 *
 * `Modifier.keepScreenOn()` 과 [PlatformIdleInhibitor] 는 앱이 떠 있는 동안만 유효하다. 그 한계를
 * 넘으려면 앱에 주어진 권한이 아니라 시스템 전역 설정을 건드려야 해서, 가능 여부와 필요한 권한이
 * 플랫폼마다 다르다.
 */
@Stable
internal interface SystemScreenAwakeState {
    /** 이 플랫폼에서 가능한가. */
    val supported: Boolean

    /** 특별 권한이 허용됐는가. [supported] 가 false 면 의미 없다. */
    val permitted: Boolean

    /** 시스템에 설정된 화면 꺼짐 시간. 읽을 수 없으면 null. */
    val screenOffTimeout: Duration?

    /** 권한 설정 화면을 연다. 이미 허용됐거나 플랫폼이 지원하지 않으면 아무 일도 하지 않는다. */
    fun requestPermission()
}

internal data object UnsupportedSystemScreenAwake : SystemScreenAwakeState {
    override val supported: Boolean = false
    override val permitted: Boolean = false
    override val screenOffTimeout: Duration? = null

    override fun requestPermission() = Unit
}

/**
 * [enabled] 인 동안 시스템 전역 화면 꺼짐 시간을 늘려 두고, 그 기능의 현재 상태를 돌려준다.
 *
 * 적용과 상태 읽기를 한 함수에 둔 이유는 둘 다 화면 수명보다 오래 살아야 해서다. 효과는 카드가
 * 스크롤 밖으로 나가도 유지되어야 하고, 권한 폴링도 한 곳에서만 돌아야 한다. 그래서 루트에서 한 번
 * 부르고 상태를 아래로 내려보낸다.
 *
 * [PlatformIdleInhibitor] 와 달리 컴포지션이 사라져도 효과를 풀지 않는다. 앱이 없는 동안 화면을
 * 켜 두는 것이 이 기능의 목적이라서, 원래 값으로 되돌리는 건 사용자가 토글을 끌 때뿐이다.
 */
@Composable
internal expect fun rememberSystemScreenAwake(enabled: Boolean): SystemScreenAwakeState
