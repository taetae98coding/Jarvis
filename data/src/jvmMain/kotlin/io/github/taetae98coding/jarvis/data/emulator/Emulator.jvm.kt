package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorPlatform
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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
// replay 덕분에 나중에 붙는 구독자는 다음 폴링을 기다리지 않는다. 프로세스 싱글턴이라 DataModule 을
// 두 번 만들어도 폴링은 한 벌이다.
private val emulatorStatuses: SharedFlow<EmulatorStatus> =
    observeByPolling(interval = PollInterval, read = ::countEmulators)
        .shareIn(emulatorScope, SharingStarted.WhileSubscribed(), replay = 1)

private val emulatorDevices: SharedFlow<List<EmulatorDevice>> =
    observeByPolling(interval = PollInterval, read = ::listDevices)
        .shareIn(emulatorScope, SharingStarted.WhileSubscribed(), replay = 1)

internal actual val emulatorDataSource: EmulatorDataSource = object : EmulatorDataSource {
    override fun observeStatus(): Flow<EmulatorStatus> = emulatorStatuses

    override fun observeDevices(): Flow<List<EmulatorDevice>> = emulatorDevices

    // 화면은 보는 사람마다 따로 찍는다. 목록과 달리 구독자가 여럿일 일이 없고, 묶어 두면 아무도
    // 보지 않는 동안에도 마지막 프레임이 남는다.
    override fun observeScreen(deviceId: String): Flow<ByteArray?> =
        observeByPolling(interval = EmulatorScreenPollInterval) { captureScreen(deviceId) }

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
        withContext(Dispatchers.IO) { sendInput(deviceId, gesture) }
    }
}

private suspend fun countEmulators(): EmulatorStatus =
    withContext(Dispatchers.IO) {
        EmulatorStatus(android = androidSummary(), ios = iosSummary())
    }

private suspend fun listDevices(): List<EmulatorDevice> =
    withContext(Dispatchers.IO) { androidDevices() + iosDevices() }

// SDK 가 없으면 0개가 아니라 "셀 수 없음" 이다. SDK 는 있는데 개별 명령이 실패한 경우는 0개로 둔다.
private fun androidSummary(): EmulatorSummary? {
    val sdk = androidSdkDirectory() ?: return null

    val avds = runCommand(listOf(emulatorBinary(sdk), "-list-avds"))
    val devices = runCommand(listOf(adbBinary(sdk), "devices"))

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

private fun androidDevices(): List<EmulatorDevice> {
    val sdk = androidSdkDirectory() ?: return emptyList()

    val names = runCommand(listOf(emulatorBinary(sdk), "-list-avds"))?.let(::parseAvdNames).orEmpty()
    val serials = runCommand(listOf(adbBinary(sdk), "devices"))?.let(::parseEmulatorSerials).orEmpty()

    // 시리얼과 AVD 이름을 잇는 공개 경로는 에뮬레이터 콘솔뿐이다. 실행 중인 수만큼 명령이 더 돌지만,
    // 이게 없으면 목록에 `emulator-5554` 만 남아 어떤 AVD 인지 알 수 없다.
    val runningNames = serials.associateWith { serial ->
        runCommand(listOf(adbBinary(sdk), "-s", serial, "emu", "avd", "name"))?.let(::parseAvdName)
    }

    val stopped = names.filterNot { name -> name in runningNames.values }
        .map { name ->
            EmulatorDevice(
                // 꺼져 있는 AVD 에는 시리얼이 없다. 켜지면 시리얼로 바뀐다.
                id = "$StoppedAvdPrefix$name",
                name = name,
                platform = EmulatorPlatform.ANDROID,
            )
        }

    val running = serials.map { serial ->
        EmulatorDevice(
            id = serial,
            name = runningNames[serial] ?: serial,
            platform = EmulatorPlatform.ANDROID,
            isRunning = true,
            canControl = true,
        )
    }

    return running + stopped
}

private fun iosDevices(): List<EmulatorDevice> {
    val simctl = simctlCommand() ?: return emptyList()
    val output = runCommand(simctl + listOf("list", "devices", "available")) ?: return emptyList()

    return parseSimulatorDevices(output)
}

private fun captureScreen(deviceId: String): ByteArray? =
    when {
        deviceId.startsWith(EmulatorSerialPrefix) -> androidSdkDirectory()?.let { sdk ->
            // `exec-out` 이라야 바이트가 그대로 나온다. `shell` 은 개행을 변환해 PNG 를 깨뜨린다.
            runCommandBytes(listOf(adbBinary(sdk), "-s", deviceId, "exec-out", "screencap", "-p"))
        }

        SimulatorUdid.matches(deviceId) -> simctlCommand()?.let { simctl ->
            // `-` 가 stdout 이다. `simctl help io` 에 적혀 있다.
            runCommandBytes(simctl + listOf("io", deviceId, "screenshot", "--type=png", "-"))
        }

        // 꺼져 있는 AVD(`avd:이름`)다. 찍을 화면이 없다.
        else -> null
    }

private fun sendInput(deviceId: String, gesture: EmulatorGesture) {
    // iOS 시뮬레이터에는 입력을 주입하는 공개 도구가 없다. 화면에서 이미 막지만 여기서도 빠진다.
    if (!deviceId.startsWith(EmulatorSerialPrefix)) return

    val sdk = androidSdkDirectory() ?: return

    val input = when (gesture) {
        is EmulatorGesture.Tap -> listOf("input", "tap", "${gesture.x}", "${gesture.y}")

        is EmulatorGesture.Swipe -> listOf(
            "input",
            "swipe",
            "${gesture.fromX}",
            "${gesture.fromY}",
            "${gesture.toX}",
            "${gesture.toY}",
            "${gesture.durationMillis}",
        )
    }

    runCommand(listOf(adbBinary(sdk), "-s", deviceId, "shell") + input)
}

// 부팅된 AVD 는 `adb devices` 에 `emulator-5554` 같은 시리얼로 나온다. 실제 기기와 네트워크 타깃은
// 시리얼 형태가 달라서, 이 접두사가 에뮬레이터와 하드웨어를 가른다.
private const val EmulatorSerialPrefix = "emulator-"

private const val StoppedAvdPrefix = "avd:"

// `emulator -list-avds` 는 AVD 이름만 한 줄에 하나씩 출력한다. 진단 메시지는 stderr 로 간다.
internal fun parseAvdNames(output: String): List<String> =
    output.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()

internal fun parseAvdCount(output: String): Int = parseAvdNames(output).size

internal fun parseRunningEmulatorCount(output: String): Int =
    output.lineSequence().count { it.startsWith(EmulatorSerialPrefix) }

// `adb devices` 는 `emulator-5554\tdevice` 형태다. `offline` 인 줄은 아직 붙는 중이라 화면을 찍을 수
// 없으므로 목록에서 뺀다(개수는 예전대로 offline 도 실행 중으로 센다).
internal fun parseEmulatorSerials(output: String): List<String> =
    output.lineSequence()
        .filter { it.startsWith(EmulatorSerialPrefix) }
        .map { it.split('\t', ' ').filter(String::isNotEmpty) }
        .filter { it.getOrNull(1) == "device" }
        .map { it.first() }
        .toList()

// `adb emu avd name` 은 이름 한 줄 뒤에 콘솔 응답인 `OK` 를 붙인다.
internal fun parseAvdName(output: String): String? =
    output.lineSequence().map(String::trim).firstOrNull { it.isNotEmpty() && it != "OK" }

internal fun parseSimulatorSummary(output: String): EmulatorSummary {
    val devices = parseSimulatorDevices(output)

    return EmulatorSummary(total = devices.size, running = devices.count(EmulatorDevice::isRunning))
}

/**
 * 기기 줄은 `    iPhone 17 (66C9B671-...-DF9528508CD7) (Shutdown)` 형태다. UDID 를 기준으로 잡아야
 * 런타임 헤더와, 괄호를 품을 수 있는 기기 이름(예: `iPad mini (A17 Pro)`)이 함께 걸리지 않는다.
 *
 * 제스처는 받을 수 없다. `simctl` 에 입력을 주입하는 하위 명령이 없어서 화면만 볼 수 있다.
 */
internal fun parseSimulatorDevices(output: String): List<EmulatorDevice> =
    SimulatorLine.findAll(output)
        .map { match ->
            EmulatorDevice(
                id = match.groupValues[2],
                name = match.groupValues[1].trim(),
                platform = EmulatorPlatform.IOS,
                isRunning = match.groupValues[3] == "Booted",
            )
        }
        .toList()

private val SimulatorLine = Regex("""^\s*(.+) \(([0-9A-F-]{36})\) \((\w+)\)\s*$""", RegexOption.MULTILINE)

private val SimulatorUdid = Regex("""[0-9A-F-]{36}""")

private fun androidSdkDirectory(): File? =
    sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        // macOS 의 Android Studio 기본 경로. 데스크탑은 macOS 만 지원한다.
        System.getProperty("user.home")?.let { "$it/Library/Android/sdk" },
    ).filterNotNull()
        .map(::File)
        .firstOrNull(File::isDirectory)

private fun adbBinary(sdk: File): String = File(sdk, "platform-tools/adb").path

private fun emulatorBinary(sdk: File): String = File(sdk, "emulator/emulator").path

private fun simctlCommand(): List<String>? {
    // `xcrun` 은 xcode-select 가 정식 Xcode 를 가리킬 때만 simctl 을 찾는다. Command Line Tools 만
    // 선택된 머신에도 Xcode.app 안에는 시뮬레이터 도구가 있으므로, 시뮬레이터가 없다고 답하기 전에
    // 기본 설치 경로를 한 번 더 본다.
    if (runCommand(listOf("xcrun", "--find", "simctl")) != null) return listOf("xcrun", "simctl")

    val bundled = File("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl")

    return if (bundled.canExecute()) listOf(bundled.path) else null
}

private const val CommandTimeoutSeconds = 10L

private fun runCommand(command: List<String>): String? = runCommandBytes(command)?.decodeToString()

private fun runCommandBytes(command: List<String>): ByteArray? =
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
                else -> output.readBytes()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()
