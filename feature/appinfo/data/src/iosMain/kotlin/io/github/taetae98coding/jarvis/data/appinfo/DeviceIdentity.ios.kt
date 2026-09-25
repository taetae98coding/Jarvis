package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.PlatformContext
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

// 시뮬레이터는 simctl 이 앱 프로세스에 자기 이름·UDID 를 환경 변수로 넣어 준다. 그 값이 device_list 와 같다.
// 실물에는 UDID 를 읽는 API 가 없고, UIDevice.name 은 iOS 16 부터 엔타이틀먼트 없이는 "iPhone" 같은 종류만 준다.
internal actual fun readDeviceIdentity(context: PlatformContext): DeviceIdentity {
    val environment = NSProcessInfo.processInfo.environment
    val device = UIDevice.currentDevice

    return DeviceIdentity(
        name = environment["SIMULATOR_DEVICE_NAME"] as? String ?: device.name,
        id = environment["SIMULATOR_UDID"] as? String ?: device.identifierForVendor?.UUIDString.orEmpty(),
    )
}
