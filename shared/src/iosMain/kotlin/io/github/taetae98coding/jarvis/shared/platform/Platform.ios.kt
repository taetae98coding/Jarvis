package io.github.taetae98coding.jarvis.shared.platform

import platform.UIKit.UIDevice

actual val platformName: String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
