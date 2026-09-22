package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository

internal class DefaultAppInfoRepository : AppInfoRepository {
    override fun getAppInfo(): AppInfo = AppInfo(version = APP_VERSION, platform = platformName)
}
