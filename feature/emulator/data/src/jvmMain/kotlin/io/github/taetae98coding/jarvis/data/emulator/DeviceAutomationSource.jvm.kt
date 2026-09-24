package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.automation.DeviceAutomation
import io.github.taetae98coding.jarvis.data.emulator.automation.AndroidAutomation
import io.github.taetae98coding.jarvis.data.emulator.automation.IosAutomation
import io.github.taetae98coding.jarvis.data.emulator.automation.JvmDeviceAutomation
import io.github.taetae98coding.jarvis.data.emulator.automation.WdaRunner
import java.io.File

internal actual val deviceAutomation: DeviceAutomation? by lazy {
    val android = androidSdkDirectory()?.let { sdk -> screenMirror?.let { AndroidAutomation(sdk, it) } }
    val ios = xcodebuild()?.let { IosAutomation(WdaRunner(it)) }

    JvmDeviceAutomation(emulatorDataSource, android, ios)
}

/**
 * 정식 Xcode 의 xcodebuild. Command Line Tools 의 `/usr/bin/xcodebuild` 는 Xcode 가 없다는 오류만 찍으므로,
 * xcode-select 가 가리키는 개발자 폴더와 기본 설치 경로에서 진짜 바이너리를 찾는다. 없으면 iOS 도구가 없다.
 */
private fun xcodebuild(): File? {
    val selected = runCommand(listOf("xcode-select", "-p"))?.trim()?.takeIf { it.endsWith("Contents/Developer") }

    return sequenceOf(selected, "/Applications/Xcode.app/Contents/Developer")
        .filterNotNull()
        .map { File(it, "usr/bin/xcodebuild") }
        .firstOrNull(File::canExecute)
}
