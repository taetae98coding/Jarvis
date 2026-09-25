package io.github.taetae98coding.jarvis.domain.appinfo

/** [deviceName]·[deviceId] 는 이 앱이 돌고 있는 기기의 것이다. 플랫폼이 값을 주지 못하면 빈 문자열이다. */
data class AppInfo(
    val version: String,
    val platform: String,
    val deviceName: String = "",
    val deviceId: String = "",
)
