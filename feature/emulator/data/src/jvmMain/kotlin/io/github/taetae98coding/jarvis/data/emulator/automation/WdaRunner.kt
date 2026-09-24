package io.github.taetae98coding.jarvis.data.emulator.automation

import io.github.taetae98coding.jarvis.automation.AutomationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

/**
 * 기기마다 WebDriverAgent 러너를 띄운다. 소스를 받아 `xcodebuild` 로 빌드하고 `test-without-building` 으로
 * 띄운다 — 미리 빌드된 러너를 `simctl launch` 로 띄우는 길은 iOS 27 에서 막혔다(docs/platform/jvm.html#mcp-server).
 * 띄운 `xcodebuild` 는 러너가 사는 동안 살아 있고, 앱이 끝날 때 함께 끝낸다.
 */
internal class WdaRunner(
    private val xcodebuild: File,
    private val root: File = File(System.getProperty("user.home"), "Library/Application Support/Jarvis/wda/$WdaVersion"),
) {
    private val runners = ConcurrentHashMap<String, Runner>()
    private val sourceLock = Mutex()
    private val buildLocks = ConcurrentHashMap<String, Mutex>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread { runners.values.forEach { it.process.destroy() } })
    }

    /** [udid] 기기에 러너를 띄우고 그 주소를 준다. 이미 떠서 답하면 그대로 쓴다. */
    suspend fun start(udid: String, physical: Boolean): WebDriverAgent {
        runners[udid]?.let { running ->
            if (running.process.isAlive && running.agent.isAlive()) return running.agent
            running.process.destroy()
            runners.remove(udid)
        }

        val team = if (physical) developmentTeam() else null
        val xctestrun = build(udid, physical, team)
        val port = if (physical) DevicePort else ServerSocket(0).use { it.localPort }

        val process = ProcessBuilder(
            xcodebuild.path, "test-without-building",
            "-xctestrun", xctestrun.path,
            "-destination", "id=$udid",
        )
            .redirectErrorStream(true)
            .apply {
                environment().putAll(developerEnvironment())
                // xcodebuild 는 TEST_RUNNER_ 를 떼고 러너 프로세스에 넘긴다.
                environment()["TEST_RUNNER_USE_PORT"] = port.toString()
            }
            .start()
            .also { it.outputStream.close() }

        val url = CompletableDeferred<String>()
        val tail = ArrayDeque<String>()
        thread(isDaemon = true, name = "wda-$udid") {
            process.inputStream.bufferedReader().forEachLine { line ->
                synchronized(tail) {
                    tail.addLast(line)
                    if (tail.size > TailLines) tail.removeFirst()
                }
                ServerUrl.find(line)?.let { url.complete(it.groupValues[1]) }
            }
            url.completeExceptionally(AutomationException(failureMessage(synchronized(tail) { tail.toList() })))
        }

        val address = withTimeoutOrNull(StartTimeout) { url.await() }
        if (address == null) {
            process.destroy()
            throw AutomationException("WebDriverAgent 가 시간 안에 뜨지 않았습니다. 기기 화면이 잠겨 있지 않은지 확인하세요.")
        }

        // 시뮬레이터는 호스트 루프백으로, 실물은 러너가 알려 준 기기 주소(같은 네트워크)로 닿는다.
        val agent = WebDriverAgent(if (physical) address else "http://127.0.0.1:$port")
        runners[udid] = Runner(process, agent)

        return agent
    }

    private suspend fun build(udid: String, physical: Boolean, team: String?): File {
        val source = source()
        val products = File(root, if (physical) "build/device-$team" else "build/simulator")
        val lock = buildLocks.getOrPut(products.path) { Mutex() }

        return lock.withLock {
            // xctestrun 은 SDK 버전마다 이름이 다르다. 이 기기의 런타임과 맞지 않으면 xcodebuild 가 알아서 실패한다.
            existingXctestrun(products, physical)?.let { return@withLock it }

            val destination = if (physical) "id=$udid" else "platform=iOS Simulator,id=$udid"
            val command = mutableListOf(
                xcodebuild.path, "build-for-testing",
                "-project", File(source, "WebDriverAgent.xcodeproj").path,
                "-scheme", "WebDriverAgentRunner",
                "-destination", destination,
                "-derivedDataPath", products.path,
            )
            if (physical) {
                command += listOf("-allowProvisioningUpdates", "DEVELOPMENT_TEAM=$team", "CODE_SIGN_STYLE=Automatic")
            } else {
                command += "CODE_SIGNING_ALLOWED=NO"
            }

            val output = run(command, BuildTimeout.inWholeSeconds)
            existingXctestrun(products, physical) ?: throw AutomationException(failureMessage(output.lines()))
        }
    }

    private fun existingXctestrun(products: File, physical: Boolean): File? =
        File(products, "Build/Products").listFiles().orEmpty()
            .filter { it.name.endsWith(".xctestrun") && (if (physical) "iphoneos" in it.name else "iphonesimulator" in it.name) }
            .maxByOrNull(File::lastModified)

    /** 소스를 한 번 받아 푼다. 실물용으로 러너의 번들 id 를 팀마다 겹치지 않게 바꿔 둔다. */
    private suspend fun source(): File =
        sourceLock.withLock {
            val source = File(root, "src/WebDriverAgent-$WdaVersion")
            if (File(source, "WebDriverAgent.xcodeproj").isDirectory) return@withLock source

            withContext(Dispatchers.IO) {
                File(root, "src").mkdirs()
                val zip = File(root, "src/WebDriverAgent-$WdaVersion.zip")
                download(SourceUrl, zip)
                val unzip = ProcessBuilder("ditto", "-x", "-k", zip.path, File(root, "src").path).redirectErrorStream(true).start()
                if (!unzip.waitFor(2, TimeUnit.MINUTES) || unzip.exitValue() != 0) throw AutomationException("WebDriverAgent 소스를 풀지 못했습니다")
                zip.delete()
            }

            if (!File(source, "WebDriverAgent.xcodeproj").isDirectory) throw AutomationException("WebDriverAgent 소스가 예상한 모양이 아닙니다")
            source
        }

    private suspend fun download(url: String, target: File) {
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        val request = HttpRequest.newBuilder(URI(url)).timeout(java.time.Duration.ofMinutes(2)).build()
        val response = runInterruptible(Dispatchers.IO) { runCatching { client.send(request, HttpResponse.BodyHandlers.ofFile(target.toPath())) }.getOrNull() }

        if (response?.statusCode() != 200) {
            target.delete()
            throw AutomationException("WebDriverAgent 소스를 내려받지 못했습니다($url). 네트워크를 확인하세요.")
        }
    }

    /**
     * 키체인의 "Apple Development" 인증서 주체의 OU 가 팀 id 다. 실물 기기용 서명에만 필요하다. 번들 id 는 팀마다
     * 달라야 해서 러너 대상의 것만 바꾼다(명령줄의 PRODUCT_BUNDLE_IDENTIFIER 는 라이브러리 대상까지 바꾼다).
     */
    private suspend fun developmentTeam(): String {
        val pem = run(listOf("security", "find-certificate", "-a", "-c", "Apple Development", "-p"), 20)
        val team = CertificateFactory.getInstance("X.509")
            .generateCertificates(ByteArrayInputStream(pem.encodeToByteArray()))
            .filterIsInstance<X509Certificate>()
            .firstNotNullOfOrNull { certificate -> OrganizationalUnit.find(certificate.subjectX500Principal.name)?.groupValues?.get(1) }
            ?: throw AutomationException("실물 iOS 에 올리려면 Xcode 의 Settings → Accounts 에 Apple 개발 팀 계정을 추가하세요(\"Apple Development\" 인증서가 없습니다).")

        val project = File(source(), "WebDriverAgent.xcodeproj/project.pbxproj")
        val text = project.readText()
        val bundleId = "io.github.taetae98coding.jarvis.wda.$team"
        if (bundleId !in text) project.writeText(text.replace("com.facebook.WebDriverAgentRunner;", "$bundleId;"))

        return team
    }

    private suspend fun run(command: List<String>, timeoutSeconds: Long): String =
        runInterruptible(Dispatchers.IO) {
            val output = File.createTempFile("jarvis-wda", ".out")
            try {
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output)
                    .apply { environment().putAll(developerEnvironment()) }
                    .start()
                process.outputStream.close()
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    throw AutomationException("${command.first().substringAfterLast('/')} 가 시간 안에 끝나지 않았습니다")
                }
                output.readText()
            } finally {
                output.delete()
            }
        }

    // Command Line Tools 만 선택된 머신에서도 Xcode.app 의 SDK·플랫폼을 쓰게 한다.
    private fun developerEnvironment(): Map<String, String> {
        val developer = xcodebuild.parentFile?.parentFile?.parentFile
        return if (developer != null && developer.path.endsWith("Contents/Developer")) mapOf("DEVELOPER_DIR" to developer.path) else emptyMap()
    }

    private class Runner(val process: Process, val agent: WebDriverAgent)

    private companion object {
        const val WdaVersion = "16.12.10"
        const val SourceUrl = "https://github.com/appium/WebDriverAgent/archive/refs/tags/v$WdaVersion.zip"

        // 실물 러너는 기기 안에서 연다. 기기마다 네트워크 주소가 다르므로 포트가 겹칠 일이 없다.
        const val DevicePort = 8100

        val StartTimeout = 90.seconds
        val BuildTimeout = 600.seconds
        const val TailLines = 40

        val ServerUrl = Regex("""ServerURLHere->(\S+)<-ServerURLHere""")
        val OrganizationalUnit = Regex("""OU=([A-Z0-9]{10})""")
    }
}

/** xcodebuild 출력 끝에서 사용자가 할 일을 알 수 있는 줄을 고른다. */
internal fun failureMessage(lines: List<String>): String {
    val text = lines.joinToString("\n")

    return when {
        "Developer Mode" in text -> "기기의 설정 → 개인정보 보호 및 보안 → 개발자 모드를 켜세요."
        "untrusted" in text.lowercase() || "not trusted" in text.lowercase() || "Invalid code signature" in text ->
            "기기의 설정 → 일반 → VPN 및 기기 관리에서 개발자 앱을 신뢰하세요."
        "No Account for Team" in text || "No profiles for" in text || "requires a development team" in text ->
            "Xcode 의 Settings → Accounts 에 개발 팀 계정을 추가하세요."
        "locked" in text.lowercase() -> "기기 잠금을 풀어 주세요."
        else -> "WebDriverAgent 를 띄우지 못했습니다: " +
            lines.lastOrNull { it.contains("error", ignoreCase = true) }?.trim()?.take(300).orEmpty().ifEmpty { lines.takeLast(3).joinToString(" ").take(300) }
    }
}
