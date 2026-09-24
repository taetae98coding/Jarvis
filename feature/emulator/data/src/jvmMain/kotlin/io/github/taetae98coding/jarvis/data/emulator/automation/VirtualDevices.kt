package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationDevice
import io.github.taetae98coding.jarvis.automation.AutomationException
import io.github.taetae98coding.jarvis.automation.AutomationPlatform
import io.github.taetae98coding.jarvis.data.emulator.SimulatorUdid
import io.github.taetae98coding.jarvis.data.emulator.StoppedAvdPrefix
import io.github.taetae98coding.jarvis.data.emulator.adbBinary
import io.github.taetae98coding.jarvis.data.emulator.androidSdkDirectory
import io.github.taetae98coding.jarvis.data.emulator.parseSimulatorDevices
import io.github.taetae98coding.jarvis.data.emulator.runCommand
import io.github.taetae98coding.jarvis.data.emulator.runCommandOutput
import io.github.taetae98coding.jarvis.data.emulator.xcodeToolCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * Claude 에게 줄 기기가 없을 때 새 AVD·시뮬레이터를 만들고, 켠 기기가 조작할 수 있게 될 때까지 기다린다
 * (docs/platform/jvm.html#device-lease).
 */
internal class VirtualDevices {
    // 이름을 고르고 만드는 사이에 다른 호출이 같은 이름을 고르지 않게 한다(docs/common/device-lease.html R8).
    private val mutex = Mutex()

    suspend fun create(platform: AutomationPlatform): AutomationDevice =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                when (platform) {
                    AutomationPlatform.ANDROID -> createAvd()
                    AutomationPlatform.IOS -> createSimulator()
                }
            }
        }

    suspend fun awaitAndroidBoot(serial: String) {
        val sdk = androidSdkDirectory() ?: throw AutomationException(NoSdkMessage)
        val command = listOf(adbBinary(sdk), "-s", serial, "shell", "getprop", "sys.boot_completed")

        while (withContext(Dispatchers.IO) { runCommandOutput(command, timeoutSeconds = 5) }?.trim() != "1") delay(1.seconds)
    }

    suspend fun awaitSimulatorBoot(udid: String) {
        val simctl = xcodeToolCommand("simctl") ?: throw AutomationException(NoRuntimeMessage)
        // `-b` 는 꺼져 있으면 켜고, 부팅이 끝날 때까지 막는다. 끝나지 않으면 도구의 시간 제한이 끊는다.
        withContext(Dispatchers.IO) { runCommandOutput(simctl + listOf("bootstatus", udid, "-b"), timeoutSeconds = 280) }
    }

    private fun createAvd(): AutomationDevice {
        val sdk = androidSdkDirectory() ?: throw AutomationException(NoSdkMessage)
        val abi = hostAbi(System.getProperty("os.arch").orEmpty())
        val image = pickSystemImage(listSystemImages(sdk), abi)
            ?: throw AutomationException("설치된 Android 시스템 이미지가 없습니다. Android Studio 의 SDK Manager 에서 $abi 시스템 이미지를 설치하세요.")

        val home = avdHome()
        // 짝이 안 맞는 찌꺼기(.ini 없는 .avd 폴더)도 이름을 차지한다. mkdirs 가 실패하지 않게 둘 다 본다.
        val existing = home.listFiles().orEmpty().map { it.name.removeSuffix(".ini").removeSuffix(".avd") }
        val name = nextName(existing, AvdNamePrefix)
        val folder = File(home, "$name.avd")

        if (!folder.mkdirs()) throw AutomationException("AVD 폴더를 만들지 못했습니다: ${folder.path}")
        File(folder, "config.ini").writeText(avdConfig(name, image))
        File(home, "$name.ini").writeText(avdIni(folder, image))

        return AutomationDevice("$StoppedAvdPrefix$name", name, AutomationPlatform.ANDROID, isPhysical = false, isRunning = false, canControl = false)
    }

    private fun createSimulator(): AutomationDevice {
        val simctl = xcodeToolCommand("simctl") ?: throw AutomationException(NoRuntimeMessage)
        val (runtime, deviceType) = runCommand(simctl + listOf("list", "runtimes", "-j"))?.let(::pickSimulatorRuntime)
            ?: throw AutomationException(NoRuntimeMessage)

        // 꺼진 것·쓸 수 없는 것도 이름을 차지하므로 available 로 거르지 않는다.
        val existing = runCommand(simctl + listOf("list", "devices"))?.let(::parseSimulatorDevices).orEmpty().map { it.name }
        val name = nextName(existing, SimulatorNamePrefix)
        val output = runCommandOutput(simctl + listOf("create", name, deviceType, runtime), timeoutSeconds = 60).orEmpty()
        val udid = SimulatorUdid.find(output)?.value
            ?: throw AutomationException("시뮬레이터를 만들지 못했습니다: ${output.trim().ifEmpty { "simctl create 실패" }}")

        return AutomationDevice(udid, name, AutomationPlatform.IOS, isPhysical = false, isRunning = false, canControl = false)
    }

    private fun listSystemImages(sdk: File): List<SystemImage> =
        File(sdk, "system-images").listFiles().orEmpty().flatMap { api ->
            api.listFiles().orEmpty().flatMap { tag ->
                tag.listFiles().orEmpty()
                    .filter { File(it, "system.img").isFile }
                    .map { abi -> SystemImage(api.name, tag.name, abi.name) }
            }
        }

    // 에뮬레이터 프로세스는 HOME 환경 변수로 AVD 를 찾는다. user.home 을 바꿔 띄운 개발 인스턴스에서도 같은 폴더를 보도록
    // 시스템 속성보다 환경 변수를 먼저 본다.
    private fun avdHome(): File {
        System.getenv("ANDROID_AVD_HOME")?.let { return File(it) }
        System.getenv("ANDROID_USER_HOME")?.let { return File(it, "avd") }

        return File(System.getenv("HOME") ?: System.getProperty("user.home"), ".android/avd")
    }

    private companion object {
        const val NoSdkMessage = "Android SDK 를 찾지 못했습니다. Android Studio 를 설치하거나 ANDROID_HOME 을 설정하세요."
        const val NoRuntimeMessage = "iOS 시뮬레이터 런타임이 없습니다. Xcode → Settings → Components 에서 설치하세요."
    }
}

internal const val AvdNamePrefix = "Jarvis_"

internal const val SimulatorNamePrefix = "Jarvis "

/** `<SDK>/system-images/<api>/<tag>/<abi>/`. [api] 는 폴더 이름(`android-36`) 그대로다. */
internal data class SystemImage(
    val api: String,
    val tag: String,
    val abi: String,
) {
    val apiLevel: Int? = Regex("""android-(\d+)""").matchEntire(api)?.groupValues?.get(1)?.toInt()
}

// 휴대폰 이미지만 고른다. TV·Wear·Automotive 태그는 목록에 없어서 고르지 않는다.
private val PhoneTags = listOf("google_apis_playstore", "google_apis", "default")

internal fun hostAbi(osArch: String): String = if (osArch == "aarch64" || osArch == "arm64") "arm64-v8a" else "x86_64"

/** 호스트 ABI 와 같고 휴대폰 태그인 것 중 API 가 가장 높은 것. 같은 API 면 [PhoneTags] 앞의 것. */
internal fun pickSystemImage(images: List<SystemImage>, abi: String): SystemImage? =
    images
        .filter { it.abi == abi && it.tag in PhoneTags && it.apiLevel != null }
        .sortedWith(compareByDescending<SystemImage> { it.apiLevel }.thenBy { PhoneTags.indexOf(it.tag) })
        .firstOrNull()

/** [prefix] 뒤에 1 부터 세어 [existing] 에 없는 첫 이름. */
internal fun nextName(existing: Collection<String>, prefix: String): String =
    generateSequence(1) { it + 1 }.map { "$prefix$it" }.first { it !in existing }

// 에뮬레이터가 채우는 하드웨어 기본값에 맡기고, 화면·메모리·키보드만 적는다. 화면은 Pixel 모양(1080×2400, 420dpi)이다.
internal fun avdConfig(name: String, image: SystemImage): String =
    listOf(
        "AvdId=$name",
        "PlayStore.enabled=${image.tag == "google_apis_playstore"}",
        "abi.type=${image.abi}",
        "avd.ini.displayname=$name",
        "avd.ini.encoding=UTF-8",
        "disk.dataPartition.size=6G",
        "hw.cpu.arch=${if (image.abi == "arm64-v8a") "arm64" else "x86_64"}",
        "hw.keyboard=yes",
        "hw.lcd.density=420",
        "hw.lcd.height=2400",
        "hw.lcd.width=1080",
        "hw.ramSize=2048",
        "image.sysdir.1=system-images/${image.api}/${image.tag}/${image.abi}/",
        "tag.id=${image.tag}",
        "target=${image.api}",
    ).joinToString("\n", postfix = "\n")

internal fun avdIni(folder: File, image: SystemImage): String =
    listOf(
        "avd.ini.encoding=UTF-8",
        "path=${folder.absolutePath}",
        "path.rel=avd/${folder.name}",
        "target=${image.api}",
    ).joinToString("\n", postfix = "\n")

/**
 * `simctl list runtimes -j` 에서 쓸 수 있는 iOS 런타임 중 버전이 가장 높은 것과, 그 런타임이 받는 첫 iPhone 종류.
 * `supportedDeviceTypes` 는 최신 기종부터 나온다. (런타임 id, 기기 종류 id).
 */
internal fun pickSimulatorRuntime(json: String): Pair<String, String>? =
    runCatching {
        Json.parseToJsonElement(json).jsonObject["runtimes"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .filter { it["isAvailable"]?.jsonPrimitive?.booleanOrNull == true && it["platform"]?.jsonPrimitive?.contentOrNull == "iOS" }
            .sortedByDescending { runtime ->
                runtime["version"]?.jsonPrimitive?.contentOrNull.orEmpty().split('.').map { it.toIntOrNull() ?: 0 }
                    .let { parts -> parts.getOrElse(0) { 0 } * 1_000_000 + parts.getOrElse(1) { 0 } * 1_000 + parts.getOrElse(2) { 0 } }
            }
            .firstNotNullOfOrNull { runtime ->
                val id = runtime["identifier"]?.jsonPrimitive?.contentOrNull ?: return@firstNotNullOfOrNull null
                val iphone = runtime["supportedDeviceTypes"]?.jsonArray.orEmpty()
                    .map { it.jsonObject }
                    .firstOrNull { it["productFamily"]?.jsonPrimitive?.contentOrNull == "iPhone" }
                    ?.get("identifier")?.jsonPrimitive?.contentOrNull
                    ?: return@firstNotNullOfOrNull null
                id to iphone
            }
    }.getOrNull()
