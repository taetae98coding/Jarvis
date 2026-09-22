package io.github.taetae98coding.jarvis.domain.screen

import kotlin.time.Duration

/**
 * 앱이 화면에 없을 때도 화면을 켜 두는 기능의 상태.
 *
 * 앱 수명 내 화면 유지는 앱에 주어진 권한으로 되지만, 그 한계를 넘으려면 시스템 전역 설정을
 * 건드려야 해서 가능 여부와 필요한 권한이 플랫폼마다 다르다.
 */
data class SystemScreenAwakeStatus(
    /** 이 플랫폼에서 가능한가. */
    val supported: Boolean = false,
    /** 특별 권한이 허용됐는가. [supported] 가 false 면 의미 없다. */
    val permitted: Boolean = false,
    /** 시스템에 설정된 화면 꺼짐 시간. 읽을 수 없으면 null. */
    val screenOffTimeout: Duration? = null,
)
