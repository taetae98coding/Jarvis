package io.github.taetae98coding.jarvis.domain.appinfo

fun interface AppInfoRepository {
    fun getAppInfo(): AppInfo
}
