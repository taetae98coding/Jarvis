package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.PlatformContext

/** 이 앱이 돌고 있는 기기의 이름과 식별자(docs/common/app-info.html R5–R7). 플랫폼이 주지 못하는 값은 빈 문자열이다. */
internal data class DeviceIdentity(
    val name: String,
    val id: String,
)

internal expect fun readDeviceIdentity(context: PlatformContext): DeviceIdentity
