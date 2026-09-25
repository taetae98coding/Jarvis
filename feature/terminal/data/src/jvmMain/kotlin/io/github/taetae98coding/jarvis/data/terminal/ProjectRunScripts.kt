package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidApp
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.AndroidVariant
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

// 실행 메뉴가 쓰는 판정·해석·스크립트. 프로세스를 띄우지 않는 순수 함수만 둔다(docs/common/terminal-run.html).

internal fun detectProjectKinds(directory: File): Set<ProjectKind> =
    buildSet {
        if (isAndroidProject(directory)) add(ProjectKind.Android)
        if (findXcodeContainer(directory) != null) add(ProjectKind.IOS)
    }

internal fun isAndroidProject(directory: File): Boolean =
    (File(directory, "settings.gradle.kts").isFile || File(directory, "settings.gradle").isFile) && File(directory, "gradlew").isFile

/**
 * 폴더와 바로 아래 폴더에서 워크스페이스를 먼저, 그다음 프로젝트를 고른다. 같은 종류면 얕은 것, 그다음 이름순이다.
 * `.xcodeproj` 안의 `project.xcworkspace` 는 내려가 보지 않아 걸리지 않는다. CocoaPods 의 `Pods/Pods.xcodeproj` 는 앱이 아니라 뺀다.
 */
internal fun findXcodeContainer(directory: File): File? {
    val top = directory.listFiles().orEmpty().toList()
    val nested = top.filter { it.isDirectory && !it.isXcodeContainer() && !it.name.startsWith(".") && it.name !in SkippedXcodeFolders }
        .flatMap { child -> child.listFiles().orEmpty().toList() }

    return (top.map { it to 0 } + nested.map { it to 1 })
        .filter { (file, _) -> file.isDirectory && file.isXcodeContainer() }
        .minWithOrNull(compareBy<Pair<File, Int>>({ (file, _) -> if (file.extension == "xcworkspace") 0 else 1 }, { it.second }, { it.first.name }))
        ?.first
}

private fun File.isXcodeContainer(): Boolean = extension == "xcworkspace" || extension == "xcodeproj"

private val SkippedXcodeFolders = setOf("Pods", "build", "node_modules")

/** 이 줄 앞머리로 init script 의 출력을 Gradle 의 다른 출력(help 문구·경고)과 가른다. */
internal const val VariantLinePrefix = "JARVIS_VARIANT"

/**
 * 앱 모듈마다 `androidComponents.onVariants` 로 변형 이름과 applicationId 를 찍는다. applicationId 를 읽을 수 없는 AGP 면
 * 빈 칸으로 찍고, 실행 스크립트가 앱 실행 단계만 건너뛴다.
 */
internal val VariantInitScript = """
    allprojects { project ->
        project.pluginManager.withPlugin("com.android.application") {
            def components = project.extensions.getByName("androidComponents")
            components.onVariants(components.selector().all()) { variant ->
                def id = ""
                try { id = variant.applicationId.getOrElse("") } catch (Throwable ignored) {}
                println("$VariantLinePrefix\t${'$'}{project.path}\t${'$'}{variant.name}\t${'$'}{id}")
            }
        }
    }
""".trimIndent()

/** `JARVIS_VARIANT\t<모듈>\t<변형>\t<applicationId>` 줄을 모듈 순서(처음 나온 순서)대로 묶는다. */
internal fun parseAndroidApps(output: String): List<AndroidApp> =
    output.lineSequence()
        .map { it.trimEnd('\r') }
        .filter { it.startsWith("$VariantLinePrefix\t") }
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            parts[1] to AndroidVariant(parts[2], parts.getOrNull(3)?.ifBlank { null })
        }
        .groupBy({ it.first }, { it.second })
        .map { (module, variants) -> AndroidApp(module, variants.distinctBy { it.name }) }

/** `xcodebuild -list -json` 의 스킴과 구성. 워크스페이스는 구성을 주지 않아 Debug·Release 로 둔다. JSON 이 아니면 null 이다. */
internal fun parseXcodeList(output: String): Pair<List<String>, List<String>>? =
    runCatching {
        val root = Json.parseToJsonElement(output.substring(output.indexOf('{'))).jsonObject
        val container = (root["project"] ?: root["workspace"])?.jsonObject ?: return null
        val schemes = container.strings("schemes")
        val configurations = container.strings("configurations").ifEmpty { DefaultIosConfigurations }

        schemes to configurations
    }.getOrNull()

private fun JsonObject.strings(key: String): List<String> =
    this[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

internal val DefaultIosConfigurations = listOf("Debug", "Release")

/** 실행 탭에 넣는 명령. 사용자 셸(zsh 의 단어 분할 등)에 흔들리지 않게 스크립트는 `/bin/sh` 가 돌린다. */
internal fun shScript(script: String): String = "/bin/sh -c ${shellQuote(script)}"

/**
 * [sdk] 는 Android SDK 폴더다. 꺼진 AVD(`avd:<이름>`)면 켜고 그 이름의 에뮬레이터를 찾아 부팅이 끝날 때까지 기다린다.
 * 설치는 `ANDROID_SERIAL` 로 그 기기 하나에만 한다(docs/common/terminal-run.html R8).
 */
internal fun androidRunScript(request: AndroidRunRequest, sdk: String?): String {
    if (sdk == null) return shScript("echo '✕ Android SDK 를 찾지 못했습니다. ANDROID_HOME 을 정하세요.'; exit 1")

    val adb = shellQuote("$sdk/platform-tools/adb")
    val task = "${request.modulePath.trimEnd(':')}:install${request.variant.name.replaceFirstChar(Char::uppercaseChar)}"
    val deviceId = request.device.id
    val applicationId = request.variant.applicationId

    val target = if (deviceId.startsWith(StoppedAvdPrefix)) {
        val avd = deviceId.removePrefix(StoppedAvdPrefix)
        """
        find_serial() {
          for s in $("${'$'}ADB" devices | awk '/^emulator-/ && ${'$'}2 == "device" {print ${'$'}1}'); do
            if [ "$("${'$'}ADB" -s "${'$'}s" emu avd name 2>/dev/null | head -n 1 | tr -d '\r')" = ${shellQuote(avd)} ]; then echo "${'$'}s"; return 0; fi
          done
          return 1
        }
        SERIAL=$(find_serial || true)
        if [ -z "${'$'}SERIAL" ]; then
          echo ${shellQuote("▶ 에뮬레이터 켜기: $avd")}
          nohup ${shellQuote("$sdk/emulator/emulator")} -avd ${shellQuote(avd)} >/dev/null 2>&1 &
          waited=0
          while [ -z "${'$'}SERIAL" ]; do
            waited=$((waited + 1))
            if [ "${'$'}waited" -gt $AvdBootTimeoutSeconds ]; then echo '✕ 에뮬레이터가 켜지지 않았습니다'; exit 1; fi
            sleep 1
            SERIAL=$(find_serial || true)
          done
        fi
        echo "▶ 부팅 기다리기: ${'$'}SERIAL"
        "${'$'}ADB" -s "${'$'}SERIAL" wait-for-device
        until [ "$("${'$'}ADB" -s "${'$'}SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 1; done
        """.trimIndent()
    } else {
        "SERIAL=${shellQuote(deviceId)}"
    }

    val launch = if (applicationId == null) {
        "echo '▶ applicationId 를 몰라 앱 실행을 건너뜁니다'"
    } else {
        // monkey 는 런처 액티비티가 없어도 종료 코드 0 으로 끝나서 출력으로 가른다.
        """
        echo ${shellQuote("▶ 앱 실행: $applicationId")}
        out=$("${'$'}ADB" -s "${'$'}SERIAL" shell monkey -p ${shellQuote(applicationId)} -c android.intent.category.LAUNCHER 1 2>&1)
        case "${'$'}out" in *"No activities"*|*"aborted"*|*"Error"*) echo "${'$'}out"; exit 1 ;; esac
        """.trimIndent()
    }

    val script = listOf(
        ScriptPrelude,
        "ADB=$adb",
        "cd ${shellQuote(request.directory)}",
        target,
        "export ANDROID_SERIAL=\"${'$'}SERIAL\"",
        "echo ${shellQuote("▶ 설치: ./gradlew $task")}",
        "./gradlew ${shellQuote(task)}",
        launch,
        "echo '✓ 실행했습니다'",
    ).joinToString("\n")

    return shScript(script)
}

/**
 * 시뮬레이터(UDID)는 부팅하고 `simctl` 로, 실물(`ios:<UDID>`)은 서명 자동 갱신을 허용해 빌드하고 `devicectl` 로 설치·실행한다
 * (docs/common/terminal-run.html R13). [derivedData] 는 이 프로젝트만의 DerivedData 폴더다.
 */
internal fun iosRunScript(request: IosRunRequest, derivedData: String): String {
    val physical = request.device.id.startsWith(PhysicalIosPrefix)
    val udid = request.device.id.removePrefix(PhysicalIosPrefix)
    val project = request.project
    val containerFlag = if (project.isWorkspace) "-workspace" else "-project"
    val sdkFolder = if (physical) "iphoneos" else "iphonesimulator"
    val products = "$derivedData/Build/Products/${request.configuration}-$sdkFolder"

    val build = listOfNotNull(
        "xcodebuild",
        containerFlag, shellQuote(project.container),
        "-scheme", shellQuote(request.scheme),
        "-configuration", shellQuote(request.configuration),
        "-destination", shellQuote("id=$udid"),
        "-derivedDataPath", shellQuote(derivedData),
        "-allowProvisioningUpdates".takeIf { physical },
        "-quiet",
        "build",
    ).joinToString(" ")

    val boot = if (physical) {
        null
    } else {
        """
        echo ${shellQuote("▶ 시뮬레이터 켜기: ${request.device.name}")}
        xcrun simctl boot ${shellQuote(udid)} 2>/dev/null || true
        xcrun simctl bootstatus ${shellQuote(udid)} >/dev/null
        """.trimIndent()
    }

    val install = if (physical) {
        """
        xcrun devicectl device install app --device ${shellQuote(udid)} "${'$'}APP"
        echo "▶ 앱 실행: ${'$'}BUNDLE"
        xcrun devicectl device process launch --device ${shellQuote(udid)} "${'$'}BUNDLE"
        """.trimIndent()
    } else {
        """
        xcrun simctl install ${shellQuote(udid)} "${'$'}APP"
        echo "▶ 앱 실행: ${'$'}BUNDLE"
        xcrun simctl launch ${shellQuote(udid)} "${'$'}BUNDLE"
        """.trimIndent()
    }

    val script = listOfNotNull(
        ScriptPrelude,
        // xcode-select 가 Command Line Tools 를 가리키면 xcodebuild·simctl 이 없다(docs/platform/jvm.html#terminal-run).
        "if ! xcrun --find xcodebuild >/dev/null 2>&1 && [ -d $XcodeDeveloperDir ]; then export DEVELOPER_DIR=$XcodeDeveloperDir; fi",
        "cd ${shellQuote(project.directory)}",
        boot,
        "echo ${shellQuote("▶ 빌드: ${request.scheme} · ${request.configuration}")}",
        build,
        "APP=$(ls -d ${shellQuote(products)}/*.app | head -n 1)",
        "BUNDLE=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' \"${'$'}APP/Info.plist\")",
        "echo \"▶ 설치: ${'$'}APP\"",
        install,
        "echo '✓ 실행했습니다'",
    ).joinToString("\n")

    return shScript(script)
}

// 단계가 실패하면 set -e 로 멈추고, 끝난 이유를 한 줄 남긴다. 탭은 그 뒤 셸로 남는다.
private val ScriptPrelude = """
    set -e
    trap 'status=${'$'}?; if [ "${'$'}status" -ne 0 ]; then echo "✕ 실패 (종료 코드 ${'$'}status)"; fi' EXIT
""".trimIndent()

internal const val XcodeDeveloperDir = "/Applications/Xcode.app/Contents/Developer"

private const val AvdBootTimeoutSeconds = 180

// emulator 기능과 같은 기기 식별자 모양이다(EmulatorDevice 의 KDoc). 기능은 서로를 의존하지 않아 접두사만 되풀이한다.
internal const val StoppedAvdPrefix = "avd:"

internal const val PhysicalIosPrefix = "ios:"
