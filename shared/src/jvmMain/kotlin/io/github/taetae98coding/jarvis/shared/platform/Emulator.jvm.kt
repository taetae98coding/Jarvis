package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

// 에뮬레이터는 이 앱 밖에서 켜지고 지워지는데 그걸 알려주는 이벤트가 없다(`adb track-devices` 는
// 있지만 `simctl` 에는 대응물이 없다). 그래서 주기적으로 다시 센다.
private val PollInterval = 5.seconds

private val emulatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// 데스크탑 UI 와 로컬 에이전트가 같은 값을 보게 묶는다. 구독자가 둘이어도 SDK 도구는 한 번만 띄우고,
// replay 덕분에 나중에 붙는 구독자는 다음 폴링을 기다리지 않는다.
private val emulatorStatuses: SharedFlow<EmulatorStatus> =
    observeByPolling(interval = PollInterval, read = ::countEmulators)
        .shareIn(emulatorScope, SharingStarted.WhileSubscribed(), replay = 1)

internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { emulatorStatuses }

private suspend fun countEmulators(): EmulatorStatus =
    withContext(Dispatchers.IO) {
        EmulatorStatus(android = androidSummary(), ios = iosSummary())
    }

// SDK 가 없으면 0개가 아니라 "셀 수 없음" 이다. SDK 는 있는데 개별 명령이 실패한 경우는 0개로 둔다.
private fun androidSummary(): EmulatorSummary? {
    val sdk = androidSdkDirectory() ?: return null

    val avds = runCommand(listOf(File(sdk, "emulator/emulator").path, "-list-avds"))
    val devices = runCommand(listOf(File(sdk, "platform-tools/adb").path, "devices"))

    return EmulatorSummary(
        total = avds?.let(::parseAvdCount) ?: 0,
        running = devices?.let(::parseRunningEmulatorCount) ?: 0,
    )
}

private fun iosSummary(): EmulatorSummary? {
    val simctl = simctlCommand() ?: return null
    val output = runCommand(simctl + listOf("list", "devices", "available")) ?: return null

    return parseSimulatorSummary(output)
}

// `emulator -list-avds` 는 AVD 이름만 한 줄에 하나씩 출력한다. 진단 메시지는 stderr 로 간다.
internal fun parseAvdCount(output: String): Int = output.lineSequence().count { it.isNotBlank() }

// 부팅된 AVD 는 `adb devices` 에 `emulator-5554` 같은 시리얼로 나온다. 실제 기기와 네트워크 타깃은
// 시리얼 형태가 달라서, 이 접두사가 에뮬레이터와 하드웨어를 가른다.
internal fun parseRunningEmulatorCount(output: String): Int =
    output.lineSequence().count { it.startsWith("emulator-") }

internal fun parseSimulatorSummary(output: String): EmulatorSummary {
    val states = SimulatorLine.findAll(output).map { it.groupValues[1] }.toList()

    return EmulatorSummary(total = states.size, running = states.count { it == "Booted" })
}

// 기기 줄은 `    iPhone 17 (66C9B671-...-DF9528508CD7) (Shutdown)` 형태다. UDID 를 기준으로 잡아야
// 런타임 헤더와, 괄호를 품을 수 있는 기기 이름(예: `iPad mini (A17 Pro)`)이 함께 걸리지 않는다.
private val SimulatorLine = Regex("""\([0-9A-F-]{36}\) \((\w+)\)""")

private fun androidSdkDirectory(): File? =
    sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        // macOS 의 Android Studio 기본 경로. 데스크탑은 macOS 만 지원한다.
        System.getProperty("user.home")?.let { "$it/Library/Android/sdk" },
    ).filterNotNull()
        .map(::File)
        .firstOrNull(File::isDirectory)

private fun simctlCommand(): List<String>? {
    // `xcrun` 은 xcode-select 가 정식 Xcode 를 가리킬 때만 simctl 을 찾는다. Command Line Tools 만
    // 선택된 머신에도 Xcode.app 안에는 시뮬레이터 도구가 있으므로, 시뮬레이터가 없다고 답하기 전에
    // 기본 설치 경로를 한 번 더 본다.
    if (runCommand(listOf("xcrun", "--find", "simctl")) != null) return listOf("xcrun", "simctl")

    val bundled = File("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl")

    return if (bundled.canExecute()) listOf(bundled.path) else null
}

private const val CommandTimeoutSeconds = 10L

private fun runCommand(command: List<String>): String? =
    runCatching {
        // 출력을 파이프가 아니라 파일로 받는다. `adb` 는 stdout 을 물려받는 데몬을 fork 하므로, 파이프를
        // 읽으면 자식이 끝난 뒤에도 블록되어 아래 타임아웃을 넘겨 버린다.
        val output = File.createTempFile("jarvis-emulator", ".out")

        try {
            val process = ProcessBuilder(command)
                .redirectOutput(output)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            process.outputStream.close()

            when {
                !process.waitFor(CommandTimeoutSeconds, TimeUnit.SECONDS) -> {
                    process.destroyForcibly()
                    null
                }

                process.exitValue() != 0 -> null
                else -> output.readText()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()
