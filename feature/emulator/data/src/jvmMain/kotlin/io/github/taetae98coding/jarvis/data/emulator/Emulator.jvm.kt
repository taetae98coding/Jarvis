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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

// 기기는 이 앱 밖에서 켜지고 꽂히는데 그걸 알려주는 이벤트가 없다(`adb track-devices` 는 있지만
// `simctl` 과 `xctrace` 에는 대응물이 없다). 그래서 주기적으로 다시 센다.
private val PollInterval = 5.seconds

private val emulatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// 데스크탑 UI 와 로컬 에이전트가 같은 값을 보게 묶는다. 구독자가 둘이어도 SDK 도구는 한 번만 띄우고,
// replay 덕분에 나중에 붙는 구독자는 다음 폴링을 기다리지 않는다. 프로세스 싱글턴이라 DataModule 을
// 두 번 만들어도 폴링은 한 벌이다.
//
// 상태 Flow 를 공유하지 않는다는 규칙의 예외다(docs/common/state-observation.html R12). 마지막 구독자가
// 떠나면 폴링을 멈추고 replay 도 비워서, 다시 붙는 구독자가 오래된 개수를 사실처럼 받지 않는다.
private val emulatorStatuses: Flow<EmulatorStatus> =
    observeByPolling(interval = PollInterval, read = ::countEmulators)
        .shareIn(emulatorScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)

private val emulatorDevices: Flow<List<EmulatorDevice>> =
    observeByPolling(interval = PollInterval, read = ::listDevices)
        .shareIn(emulatorScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)

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

    override suspend fun launch(deviceId: String) {
        withContext(Dispatchers.IO) { launchDevice(deviceId) }
    }

    override suspend fun wake(deviceId: String) {
        withContext(Dispatchers.IO) { wakeDevice(deviceId) }
    }
}

private suspend fun countEmulators(): EmulatorStatus =
    withContext(Dispatchers.IO) {
        EmulatorStatus(android = androidSummary(), ios = iosSummary())
    }

private suspend fun listDevices(): List<EmulatorDevice> =
    withContext(Dispatchers.IO) {
        // 정렬이 안정적이라 플랫폼 안의 순서는 그대로 두고 실행 중인 것만 위로 올라온다.
        (androidDevices() + iosDevices()).sortedByDescending(EmulatorDevice::isRunning)
    }

// SDK 가 없으면 0개가 아니라 "셀 수 없음" 이다. SDK 는 있는데 개별 명령이 실패한 경우는 0개로 둔다.
private fun androidSummary(): EmulatorSummary? {
    val sdk = androidSdkDirectory() ?: return null

    val avds = runCommand(listOf(emulatorBinary(sdk), "-list-avds"))
    val devices = runCommand(listOf(adbBinary(sdk), "devices"))

    return EmulatorSummary(
        total = avds?.let(::parseAvdCount) ?: 0,
        running = devices?.let(::parseRunningEmulatorCount) ?: 0,
        physical = devices?.let(::parsePhysicalSerials)?.size ?: 0,
    )
}

private fun iosSummary(): EmulatorSummary? {
    val simctl = xcodeToolCommand("simctl") ?: return null
    val output = runCommand(simctl + listOf("list", "devices", "available")) ?: return null

    return parseSimulatorSummary(output).copy(physical = physicalIosDevices().size)
}

private fun androidDevices(): List<EmulatorDevice> {
    val sdk = androidSdkDirectory() ?: return emptyList()

    val names = runCommand(listOf(emulatorBinary(sdk), "-list-avds"))?.let(::parseAvdNames).orEmpty()
    val devices = runCommand(listOf(adbBinary(sdk), "devices"))
    val serials = devices?.let(::parseEmulatorSerials).orEmpty()
    val physicalSerials = devices?.let(::parsePhysicalSerials).orEmpty()

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
                canLaunch = true,
            )
        }

    val running = serials.map { serial ->
        EmulatorDevice(
            id = serial,
            name = runningNames[serial] ?: serial,
            platform = EmulatorPlatform.ANDROID,
            isRunning = true,
            isAsleep = isScreenOff(sdk, serial),
            canStream = true,
            canControl = true,
        )
    }

    // 실물 기기도 adb 시리얼이라 화면과 제스처가 에뮬레이터와 똑같이 동작한다. 이름만 기기에
    // 물어봐야 한다.
    val physical = physicalSerials.map { serial ->
        EmulatorDevice(
            id = serial,
            name = runCommand(listOf(adbBinary(sdk), "-s", serial, "shell", "getprop", "ro.product.model"))
                ?.let(::parseDeviceModel)
                ?: serial,
            platform = EmulatorPlatform.ANDROID,
            isPhysical = true,
            isRunning = true,
            isAsleep = isScreenOff(sdk, serial),
            canStream = true,
            canControl = true,
        )
    }

    return running + physical + stopped
}

private fun iosDevices(): List<EmulatorDevice> {
    val simctl = xcodeToolCommand("simctl") ?: return emptyList()
    val output = runCommand(simctl + listOf("list", "devices", "available")) ?: return emptyList()

    return parseSimulatorDevices(output) + physicalIosDevices()
}

// 연결된 실물 iOS 기기는 `xctrace` 만 나열해 준다.
private fun physicalIosDevices(): List<EmulatorDevice> {
    val xctrace = xcodeToolCommand("xctrace") ?: return emptyList()

    return runCommand(xctrace + listOf("list", "devices"), mergeError = true)
        ?.let(::parsePhysicalIosDevices)
        .orEmpty()
}

// 화면이 꺼져 있어도 screencap 은 오류가 아니라 검은 그림을 준다. 그걸 화면에서 구분해 주려면
// 상태를 따로 물어보는 수밖에 없다.
private fun isScreenOff(sdk: File, serial: String): Boolean =
    runCommand(listOf(adbBinary(sdk), "-s", serial, "shell", "dumpsys", "deviceidle", "get", "screen"))
        ?.let(::parseScreenOn) == false

// 폴링은 read 를 수집하는 쪽 디스패처에서 부르고, 스트리밍 화면은 EDT 에서 수집한다. 여기서 옮기지 않으면
// 한 장(0.3~0.6초)마다 UI 가 멈춘다.
private suspend fun captureScreen(deviceId: String): ByteArray? =
    withContext(Dispatchers.IO) { captureScreenBlocking(deviceId) }

private fun captureScreenBlocking(deviceId: String): ByteArray? =
    when {
        isAdbSerial(deviceId) -> androidSdkDirectory()?.let { sdk ->
            // `exec-out` 이라야 바이트가 그대로 나온다. `shell` 은 개행을 변환해 PNG 를 깨뜨린다.
            runCommandBytes(listOf(adbBinary(sdk), "-s", deviceId, "exec-out", "screencap", "-p"))
        }

        SimulatorUdid.matches(deviceId) -> xcodeToolCommand("simctl")?.let { simctl ->
            // `-` 가 stdout 이다. `simctl help io` 에 적혀 있다.
            runCommandBytes(simctl + listOf("io", deviceId, "screenshot", "--type=png", "-"))
        }

        // 꺼져 있는 AVD(`avd:이름`)와 실물 iOS 기기(`ios:UDID`)다. 전자는 찍을 화면이 없고, 후자는
        // 찍을 수 있는 공개 도구가 없다.
        else -> null
    }

private fun sendInput(deviceId: String, gesture: EmulatorGesture) {
    // iOS 에는 시뮬레이터에도 실물 기기에도 입력을 주입하는 공개 도구가 없다. 화면에서 이미 막지만
    // 여기서도 빠진다.
    if (!isAdbSerial(deviceId)) return

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

private fun wakeDevice(deviceId: String) {
    // 깨우는 것도 입력 주입이라 제스처와 조건이 같다. iOS 는 시뮬레이터에도 실물 기기에도 방법이 없다.
    if (!isAdbSerial(deviceId)) return

    val sdk = androidSdkDirectory() ?: return

    // KEYCODE_POWER 는 토글이라 이미 켜진 화면을 끈다. WAKEUP 은 몇 번을 보내도 켜기만 한다.
    runCommand(listOf(adbBinary(sdk), "-s", deviceId, "shell", "input", "keyevent", "KEYCODE_WAKEUP"))
}

private fun launchDevice(deviceId: String) {
    when {
        deviceId.startsWith(StoppedAvdPrefix) -> androidSdkDirectory()?.let { sdk ->
            startDetached(listOf(emulatorBinary(sdk), "-avd", deviceId.removePrefix(StoppedAvdPrefix)))
        }

        SimulatorUdid.matches(deviceId) -> xcodeToolCommand("simctl")?.let { simctl ->
            runCommand(simctl + listOf("boot", deviceId))
            // `boot` 는 기기만 띄우고 창은 열지 않는다. 순서를 바꾸면 Simulator.app 이 마지막으로
            // 쓰던 기기를 대신 띄운다.
            runCommand(listOf("open", "-a", "Simulator"))
        }

        // 실행 중인 기기와 실물 기기다. 켤 것이 없다.
        else -> Unit
    }
}

/**
 * `adb -s` 에 그대로 넘길 수 있는 식별자인지. 나머지 세 종류는 접두사나 UDID 모양으로 갈린다.
 * 에뮬레이터와 실물 Android 기기는 여기서 구분하지 않는다. 화면도 제스처도 같은 명령을 쓴다.
 */
private fun isAdbSerial(deviceId: String): Boolean =
    !deviceId.startsWith(StoppedAvdPrefix) &&
        !deviceId.startsWith(PhysicalIosPrefix) &&
        !SimulatorUdid.matches(deviceId)

// 부팅된 AVD 는 `adb devices` 에 `emulator-5554` 같은 시리얼로 나온다. 이 접두사가 에뮬레이터와
// 실물 기기를 가른다.
private const val EmulatorSerialPrefix = "emulator-"

private const val StoppedAvdPrefix = "avd:"

// 실물 iOS 기기의 UDID(`00008030-001A2B3C11E8802E`)는 Android 시리얼과 모양으로 갈리지 않는다.
// 시뮬레이터 UUID 와 달리 접두사를 붙여야 어느 도구를 부를지 정할 수 있다.
private const val PhysicalIosPrefix = "ios:"

// `emulator -list-avds` 는 AVD 이름만 한 줄에 하나씩 출력한다. 진단 메시지는 stderr 로 간다.
internal fun parseAvdNames(output: String): List<String> =
    output.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()

internal fun parseAvdCount(output: String): Int = parseAvdNames(output).size

internal fun parseRunningEmulatorCount(output: String): Int =
    output.lineSequence().count { it.startsWith(EmulatorSerialPrefix) }

/**
 * `adb devices` 는 `emulator-5554\tdevice` 형태다. `offline` 이나 `unauthorized` 인 줄은 화면을 찍을
 * 수 없으므로 목록에서 뺀다(에뮬레이터 개수는 예전대로 offline 도 실행 중으로 센다).
 *
 * 공백이 아니라 탭으로만 자른다. 무선 디버깅으로 붙은 기기의 시리얼은 mDNS 이름이라 공백을 품을 수
 * 있고(`adb-R54T202XEHN-Y2yH0N (2)._adb-tls-connect._tcp`), 공백으로 자르면 그런 줄이 통째로 빠진다.
 */
internal fun parseAttachedSerials(output: String): List<String> =
    output.lineSequence()
        .map { it.split('\t') }
        .filter { it.size >= 2 && it[1].trim() == "device" }
        .map(List<String>::first)
        .toList()

internal fun parseEmulatorSerials(output: String): List<String> =
    parseAttachedSerials(output).filter { it.startsWith(EmulatorSerialPrefix) }

// 에뮬레이터가 아닌 시리얼은 전부 실물 기기다. USB 로 붙은 기기와 `adb connect` 로 붙은 기기
// (`192.168.0.10:5555`)를 구분하지 않는다. 화면과 제스처는 어느 쪽이든 같은 명령으로 간다.
internal fun parsePhysicalSerials(output: String): List<String> =
    parseAttachedSerials(output).filterNot { it.startsWith(EmulatorSerialPrefix) }

// `adb emu avd name` 은 이름 한 줄 뒤에 콘솔 응답인 `OK` 를 붙인다.
internal fun parseAvdName(output: String): String? =
    output.lineSequence().map(String::trim).firstOrNull { it.isNotEmpty() && it != "OK" }

/**
 * `dumpsys deviceidle get screen` 은 `true` / `false` 한 줄을 준다. 둘 다 아니면 모르는 것이다
 * (null). 모르는 것을 "꺼짐" 으로 표시하면 깨울 수도 없는 기기에 버튼이 붙는다.
 */
internal fun parseScreenOn(output: String): Boolean? =
    when (output.trim()) {
        "true" -> true
        "false" -> false
        else -> null
    }

// `getprop` 은 값 한 줄을 준다. 속성이 없는 기기에서는 빈 줄이라 시리얼로 되돌려야 한다.
internal fun parseDeviceModel(output: String): String? =
    output.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty)

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
            val booted = match.groupValues[3] == "Booted"

            EmulatorDevice(
                id = match.groupValues[2],
                name = match.groupValues[1].trim(),
                platform = EmulatorPlatform.IOS,
                isRunning = booted,
                canStream = booted,
                canLaunch = !booted,
            )
        }
        .toList()

/**
 * `xctrace list devices` 의 `== Devices ==` 아래에 연결된 실물 기기가
 * `taetae의 iPhone (18.5) (00008130-000A1C2E0298001C)` 형태로 나온다. 괄호 묶음이 하나뿐인 줄은 이
 * Mac 자신이라 정규식에서 걸러진다. 그다음 `==` 로 시작하는 줄부터는 시뮬레이터와 연결이 끊긴
 * 기기라 읽지 않는다.
 *
 * 화면도 제스처도 줄 수 없다. 실물 iOS 기기를 찍는 공개 도구가 없어서 목록에만 쓴다.
 */
internal fun parsePhysicalIosDevices(output: String): List<EmulatorDevice> =
    output.lineSequence()
        .dropWhile { it.trim() != "== Devices ==" }
        .drop(1)
        .takeWhile { !it.trim().startsWith("==") }
        .mapNotNull { line -> PhysicalIosLine.matchEntire(line.trim()) }
        .map { match ->
            EmulatorDevice(
                id = "$PhysicalIosPrefix${match.groupValues[3]}",
                name = match.groupValues[1].trim(),
                platform = EmulatorPlatform.IOS,
                isPhysical = true,
                isRunning = true,
            )
        }
        .toList()

private val SimulatorLine = Regex("""^\s*(.+) \(([0-9A-F-]{36})\) \((\w+)\)\s*$""", RegexOption.MULTILINE)

private val SimulatorUdid = Regex("""[0-9A-F-]{36}""")

private val PhysicalIosLine = Regex("""(.+) \((\d[\d.]*)\) \(([0-9A-Fa-f-]{25,})\)""")

internal fun androidSdkDirectory(): File? =
    sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        // macOS 의 Android Studio 기본 경로. 데스크탑은 macOS 만 지원한다.
        System.getProperty("user.home")?.let { "$it/Library/Android/sdk" },
    ).filterNotNull()
        .map(::File)
        .firstOrNull(File::isDirectory)

internal fun adbBinary(sdk: File): String = File(sdk, "platform-tools/adb").path

private fun emulatorBinary(sdk: File): String = File(sdk, "emulator/emulator").path

private fun xcodeToolCommand(tool: String): List<String>? {
    // `xcrun` 은 xcode-select 가 정식 Xcode 를 가리킬 때만 simctl·xctrace 를 찾는다. Command Line Tools
    // 만 선택된 머신에도 Xcode.app 안에는 두 도구가 있으므로, 없다고 답하기 전에 기본 설치 경로를 한 번
    // 더 본다. 예전에는 simctl 에만 이 대비가 있어서 시뮬레이터는 보이는데 실물 iPhone 은 0개였다.
    if (runCommand(listOf("xcrun", "--find", tool)) != null) return listOf("xcrun", tool)

    val bundled = File("/Applications/Xcode.app/Contents/Developer/usr/bin/$tool")

    return if (bundled.canExecute()) listOf(bundled.path) else null
}

private const val CommandTimeoutSeconds = 10L

internal fun runCommand(command: List<String>, mergeError: Boolean = false): String? =
    runCommandBytes(command, mergeError)?.decodeToString()

/**
 * 두 스트림을 합친 출력을 종료 코드와 상관없이 준다. 실패 이유가 출력에 있는 명령(`adb pair`)에 쓴다.
 * 시간 안에 끝나지 않았거나 띄우지 못했으면 null 이다.
 */
internal fun runCommandOutput(command: List<String>, timeoutSeconds: Long): String? =
    runCommandBytes(command, mergeError = true, timeoutSeconds = timeoutSeconds, requireSuccess = false)
        ?.decodeToString()

private fun runCommandBytes(
    command: List<String>,
    mergeError: Boolean = false,
    timeoutSeconds: Long = CommandTimeoutSeconds,
    requireSuccess: Boolean = true,
): ByteArray? =
    runCatching {
        // 출력을 파이프가 아니라 파일로 받는다. `adb` 는 stdout 을 물려받는 데몬을 fork 하므로, 파이프를
        // 읽으면 자식이 끝난 뒤에도 블록되어 아래 타임아웃을 넘겨 버린다.
        val output = File.createTempFile("jarvis-emulator", ".out")

        try {
            val process = ProcessBuilder(command)
                .redirectOutput(output)
                // xctrace 는 버전에 따라 목록을 stderr 로 내보낸다. 형태가 맞는 줄만 골라 읽으므로
                // 두 스트림이 섞여도 결과가 달라지지 않는다.
                .apply { if (mergeError) redirectErrorStream(true) else redirectError(ProcessBuilder.Redirect.DISCARD) }
                .start()

            process.outputStream.close()

            when {
                !process.waitFor(timeoutSeconds, TimeUnit.SECONDS) -> {
                    process.destroyForcibly()
                    null
                }

                requireSuccess && process.exitValue() != 0 -> null
                else -> output.readBytes()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()

/**
 * 끝나기를 기다리지 않고 띄운다. `emulator -avd` 는 에뮬레이터가 꺼질 때까지 살아 있어서 기다릴 수
 * 없고, 출력을 파이프로 받아 두면 아무도 읽지 않는 버퍼가 차서 에뮬레이터가 멈춘다. JVM 이 먼저
 * 끝나도 자식은 살아남는다.
 */
private fun startDetached(command: List<String>) {
    runCatching {
        ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    }
}
