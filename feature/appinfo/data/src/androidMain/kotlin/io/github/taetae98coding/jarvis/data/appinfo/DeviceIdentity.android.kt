package io.github.taetae98coding.jarvis.data.appinfo

import android.os.Build
import android.provider.Settings
import io.github.taetae98coding.jarvis.data.PlatformContext

// 모델명은 adb 의 ro.product.model 과 같아서 데스크톱 기기 목록의 이름과 맞는다. adb 시리얼(Build.getSerial)은
// API 29 부터 시스템 앱 전용 권한이 필요해서 읽을 수 없다(docs/platform/android.html#app-info).
internal actual fun readDeviceIdentity(context: PlatformContext): DeviceIdentity =
    DeviceIdentity(
        name = Build.MODEL.orEmpty(),
        id = Settings.Secure.getString(context.context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty(),
    )
