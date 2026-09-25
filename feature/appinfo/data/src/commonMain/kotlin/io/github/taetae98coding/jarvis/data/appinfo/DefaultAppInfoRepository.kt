package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository

internal class DefaultAppInfoRepository(
    private val context: PlatformContext,
) : AppInfoRepository {
    // 데스크톱은 프로세스를 띄워 읽으므로 화면이 다시 만들어질 때마다 읽지 않는다.
    private val device: DeviceIdentity by lazy { readDeviceIdentity(context) }

    override fun getAppInfo(): AppInfo =
        AppInfo(
            version = APP_VERSION,
            platform = platformName,
            deviceName = device.name,
            deviceId = device.id,
        )
}
