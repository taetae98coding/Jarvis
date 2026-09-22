package io.github.taetae98coding.jarvis.data.appinfo

import platform.UIKit.UIDevice

internal actual val platformName: String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
