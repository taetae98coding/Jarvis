package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidApp
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.AndroidVariant
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.IosProject
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import io.github.taetae98coding.jarvis.domain.terminal.RunDevice
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectRunScriptsTest {
    private fun folder(vararg files: String): File {
        val root = Files.createTempDirectory("jarvis-project").toFile()
        files.forEach { path ->
            val file = File(root, path)
            if (path.endsWith("/")) file.mkdirs() else file.apply { parentFile.mkdirs() }.writeText("")
        }
        return root
    }

    @Test
    fun androidNeedsSettingsAndGradlew() {
        assertEquals(setOf(ProjectKind.Android), detectProjectKinds(folder("settings.gradle.kts", "gradlew")))
        assertEquals(setOf(ProjectKind.Android), detectProjectKinds(folder("settings.gradle", "gradlew")))
        assertEquals(emptySet(), detectProjectKinds(folder("settings.gradle.kts")))
        assertEquals(emptySet(), detectProjectKinds(folder("gradlew")))
    }

    @Test
    fun xcodeContainersAreFoundOneLevelDownAndWorkspacesWin() {
        val kmp = folder("settings.gradle.kts", "gradlew", "iosApp/iosApp.xcodeproj/project.pbxproj", "iosApp/iosApp.xcodeproj/project.xcworkspace/")
        assertEquals(setOf(ProjectKind.Android, ProjectKind.IOS), detectProjectKinds(kmp))
        assertEquals("iosApp.xcodeproj", findXcodeContainer(kmp)?.name)

        val pods = folder("App.xcodeproj/", "App.xcworkspace/", "Pods/Pods.xcodeproj/")
        assertEquals("App.xcworkspace", findXcodeContainer(pods)?.name)

        assertNull(findXcodeContainer(folder("a/b/Deep.xcodeproj/")))
    }

    @Test
    fun variantLinesAreGroupedByModule() {
        val output = """
            > Configure project
            JARVIS_VARIANT	:androidApp	debug	io.github.app
            JARVIS_VARIANT	:androidApp	release	io.github.app
            JARVIS_VARIANT	:wear	freeDebug	
            Welcome to Gradle 9.7.1.
        """.trimIndent()

        assertEquals(
            listOf(
                AndroidApp(":androidApp", listOf(AndroidVariant("debug", "io.github.app"), AndroidVariant("release", "io.github.app"))),
                AndroidApp(":wear", listOf(AndroidVariant("freeDebug", null))),
            ),
            parseAndroidApps(output),
        )
    }

    @Test
    fun xcodeListReadsProjectsAndWorkspaces() {
        val project = """{ "project" : { "configurations" : ["Debug", "Release", "Beta"], "name" : "iosApp", "schemes" : ["iosApp"], "targets" : ["iosApp"] } }"""
        assertEquals(listOf("iosApp") to listOf("Debug", "Release", "Beta"), parseXcodeList(project))

        val workspace = "Command line invocation:\n{ \"workspace\" : { \"name\" : \"App\", \"schemes\" : [\"App\", \"Pods-App\"] } }"
        assertEquals(listOf("App", "Pods-App") to DefaultIosConfigurations, parseXcodeList(workspace))

        assertNull(parseXcodeList("xcodebuild: error: not a project"))
    }

    private val variant = AndroidVariant("freeDebug", "io.github.app")

    @Test
    fun androidScriptInstallsOnTheChosenSerialAndLaunches() {
        val device = RunDevice("adb-R54T (2)._adb-tls-connect._tcp", "SM-X906N", DevicePlatform.Android, isPhysical = true)
        val script = androidRunScript(AndroidRunRequest("/repo it's", ":androidApp", variant, device), "/sdk")

        assertTrue(script.startsWith("/bin/sh -c '"))
        val body = unwrap(script)
        assertTrue("SERIAL='adb-R54T (2)._adb-tls-connect._tcp'" in body)
        assertTrue("export ANDROID_SERIAL=\"\$SERIAL\"" in body)
        assertTrue("./gradlew ':androidApp:installFreeDebug'" in body)
        assertTrue("monkey -p 'io.github.app'" in body)
        assertTrue("cd '/repo it'\\''s'" in body)
        assertFalse("emulator" in body)
        assertSyntax(body)
    }

    @Test
    fun androidScriptBootsAStoppedAvdAndWaitsForIt() {
        val device = RunDevice("avd:Pixel_9", "Pixel_9", DevicePlatform.Android, isRunning = false)
        val body = unwrap(androidRunScript(AndroidRunRequest("/repo", ":androidApp", variant, device), "/sdk"))

        assertTrue("'/sdk/emulator/emulator' -avd 'Pixel_9'" in body)
        assertTrue("emu avd name" in body)
        assertTrue("sys.boot_completed" in body)
        assertSyntax(body)
    }

    @Test
    fun androidScriptWithoutSdkOrApplicationId() {
        val device = RunDevice("emulator-5554", "Pixel", DevicePlatform.Android)
        assertTrue("SDK" in androidRunScript(AndroidRunRequest("/repo", ":app", variant, device), null))

        val body = unwrap(androidRunScript(AndroidRunRequest("/repo", ":app", AndroidVariant("debug", null), device), "/sdk"))
        assertFalse("monkey" in body)
        assertSyntax(body)
    }

    private val project = IosProject("/repo", "/repo/iosApp/iosApp.xcodeproj", isWorkspace = false, listOf("iosApp"), listOf("Debug", "Release"))

    @Test
    fun iosSimulatorScriptBootsBuildsInstallsAndLaunches() {
        val device = RunDevice("66C9B671-0000-0000-0000-DF9528508CD7", "iPhone 17", DevicePlatform.IOS, isRunning = false)
        val body = unwrap(iosRunScript(IosRunRequest(project, "iosApp", "Debug", device), "/dd"))

        assertTrue("xcrun simctl boot '66C9B671-0000-0000-0000-DF9528508CD7'" in body)
        assertTrue("xcodebuild -project '/repo/iosApp/iosApp.xcodeproj' -scheme 'iosApp' -configuration 'Debug' -destination 'id=66C9B671-0000-0000-0000-DF9528508CD7' -derivedDataPath '/dd' -quiet build" in body)
        assertTrue("'/dd/Build/Products/Debug-iphonesimulator'/*.app" in body)
        assertTrue("xcrun simctl launch" in body)
        assertFalse("devicectl" in body)
        assertSyntax(body)
    }

    @Test
    fun iosDeviceScriptUsesDevicectlAndProvisioningUpdates() {
        val device = RunDevice("ios:00008130-000A1C2E0298001C", "iPhone", DevicePlatform.IOS, isPhysical = true, canMirror = false)
        val body = unwrap(iosRunScript(IosRunRequest(project.copy(container = "/repo/App.xcworkspace", isWorkspace = true), "App", "Release", device), "/dd"))

        assertTrue("-workspace '/repo/App.xcworkspace'" in body)
        assertTrue("-destination 'id=00008130-000A1C2E0298001C'" in body)
        assertTrue("-allowProvisioningUpdates" in body)
        assertTrue("'/dd/Build/Products/Release-iphoneos'/*.app" in body)
        assertTrue("devicectl device install app --device '00008130-000A1C2E0298001C'" in body)
        assertFalse("simctl" in body)
        assertSyntax(body)
    }

    // shScript 가 감싼 따옴표를 벗긴다. 테스트의 경로에 든 작은따옴표는 '\'' 로 이어 붙어 있다.
    private fun unwrap(command: String): String =
        command.removePrefix("/bin/sh -c '").removeSuffix("'").replace("'\\''", "'")

    private fun assertSyntax(body: String) {
        val process = ProcessBuilder("/bin/sh", "-n", "-c", body).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), output)
    }
}
