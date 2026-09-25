package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File

/** [searchPath] 는 루트에서 compile DB 폴더까지의 상대 경로, [environment] 는 그 SDK 를 가진 툴체인 환경이다. */
internal data class SwiftCompileDatabase(val searchPath: String, val environment: Map<String, String>)

/**
 * `Package.swift` 가 없는 폴더(Xcode 프로젝트)는 sourcekit-lsp 가 빌드 설정을 몰라 파일 하나만 본다. 루트 아래 `.swift` 를 한
 * 모듈로 묶은 `compile_commands.json` 을 [cacheRoot] 에 만든다(docs/platform/jvm.html#terminal-code-navigation). SDK 를 찾지
 * 못하면 null 이고 서버는 파일 하나만 본다.
 */
internal fun swiftCompileDatabase(root: File, cacheRoot: File, base: Map<String, String>): SwiftCompileDatabase? {
    val project = root.listFiles().orEmpty().filter { it.name.endsWith(".xcodeproj") }.map { File(it, "project.pbxproj") }.firstOrNull { it.isFile }
    val settings = project?.let { runCatching { it.readText() }.getOrNull() }.orEmpty()
    val ios = "SDKROOT = iphoneos" in settings
    val sdk = if (ios) "iphonesimulator" else "macosx"
    val (sdkPath, environment) = sdkPath(sdk, base) ?: return null
    val deployment = Regex(if (ios) """IPHONEOS_DEPLOYMENT_TARGET = ([0-9.]+);""" else """MACOSX_DEPLOYMENT_TARGET = ([0-9.]+);""")
        .find(settings)?.groupValues?.get(1) ?: if (ios) "17.0" else "14.0"
    val arch = if (System.getProperty("os.arch") == "aarch64") "arm64" else "x86_64"
    val target = if (ios) "$arch-apple-ios$deployment-simulator" else "$arch-apple-macos$deployment"

    val files = swiftFiles(root)
    if (files.isEmpty()) return null

    val module = root.name.filter { it.isLetterOrDigit() || it == '_' }.ifEmpty { "Module" }
    val arguments = listOf("swiftc", "-module-name", module, "-sdk", sdkPath, "-target", target) + files.map { it.path }
    val directory = File(cacheRoot, root.canonicalPath.hashCode().toUInt().toString(16)).apply { mkdirs() }
    val json = buildJsonArray {
        files.forEach { file ->
            addJsonObject {
                put("directory", root.path)
                put("file", file.path)
                putJsonArray("arguments") { arguments.forEach { add(JsonPrimitive(it)) } }
            }
        }
    }
    File(directory, "compile_commands.json").writeText(json.toString())

    // sourcekit-lsp 는 이 경로를 루트에서의 상대 경로로만 받는다. 절대 경로는 무시됐다(Swift 6.4).
    return SwiftCompileDatabase(directory.toPath().let { root.toPath().relativize(it).toString() }, environment)
}

// Command Line Tools 에는 iOS SDK 가 없다. 없으면 Xcode.app 의 툴체인으로 다시 찾는다.
private fun sdkPath(sdk: String, base: Map<String, String>): Pair<String, Map<String, String>>? {
    runQuietly(listOf("/usr/bin/xcrun", "--sdk", sdk, "--show-sdk-path"), base)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it to base }
    if (!File(XcodeDeveloperDir).isDirectory || base["DEVELOPER_DIR"] == XcodeDeveloperDir) return null

    val xcode = base + ("DEVELOPER_DIR" to XcodeDeveloperDir)

    return runQuietly(listOf("/usr/bin/xcrun", "--sdk", sdk, "--show-sdk-path"), xcode)?.trim()?.takeIf { it.isNotEmpty() }?.let { it to xcode }
}

private fun swiftFiles(root: File): List<File> =
    root.walkTopDown()
        .onEnter { it == root || it.name !in SkippedSwiftDirectories }
        .filter { it.isFile && it.extension == "swift" }
        .take(SwiftFilesMax)
        .toList()

private val SkippedSwiftDirectories = setOf("build", ".build", "DerivedData", "Pods", ".git", "node_modules")

private const val SwiftFilesMax = 5_000
