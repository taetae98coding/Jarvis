package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isWritable
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * 설치된 [bundle] 을 새 DMG 의 앱으로 바꾼다. 앱이 살아 있는 동안 번들 옆에 새 앱을 풀어 두고, 앱이 끝난 뒤
 * 셸 스크립트가 두 번의 mv 로 바꾸고 다시 띄운다(docs/platform/jvm.html#app-update).
 */
internal class MacAppUpdater(
    private val bundle: Path,
    private val releaseUrl: String,
    private val userAgent: String,
) : AppUpdater {
    private val client: HttpClient = HttpClient.newBuilder()
        // 자산 주소는 objects.githubusercontent.com 으로 넘겨준다.
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    // 304 는 토큰 없는 API 한도에 세지 않는다. 같은 응답이면 앞 본문을 그대로 쓴다.
    @Volatile private var cached: Pair<String, String>? = null

    override suspend fun fetchLatestRelease(): String? =
        runCatching {
            val request = HttpRequest.newBuilder(URI(releaseUrl))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(20))
                .apply { cached?.let { (etag, _) -> header("If-None-Match", etag) } }
                .build()
            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()

            when (response.statusCode()) {
                200 -> response.body().also { body ->
                    cached = response.headers().firstValue("ETag").orElse(null)?.let { it to body }
                }
                304 -> cached?.second
                else -> null
            }
        }.getOrNull()

    override suspend fun install(release: AppRelease) = withContext(Dispatchers.IO) {
        val folder = bundle.parent
        if (bundle.absolutePathString().contains("/AppTranslocation/") || !folder.isWritable()) {
            throw IOException("${folder.absolutePathString()} 에 쓸 수 없습니다. Jarvis 를 응용 프로그램 폴더로 옮긴 뒤 다시 시도하세요.")
        }

        val staged = folder.resolve(".Jarvis-update.app")
        val work = Files.createTempDirectory("jarvis-update")
        try {
            val dmg = download(release.downloadUrl, work.resolve("Jarvis.dmg"))
            val expected = download(release.checksumUrl, work.resolve("Jarvis.dmg.sha256"))
                .readText().trim().substringBefore(' ').lowercase()
            if (sha256(dmg) != expected) throw IOException("받은 파일의 체크섬이 맞지 않습니다.")

            staged.deleteRecursivelyIfExists()
            extractApp(dmg, work.resolve("mnt"), staged)
            launchReplacer(staged, work)
        } catch (e: Throwable) {
            staged.deleteRecursivelyIfExists()
            work.deleteRecursivelyIfExists()
            throw e
        }

        requestAppQuit()
    }

    private suspend fun download(url: String, target: Path): Path {
        val request = HttpRequest.newBuilder(URI(url))
            .header("User-Agent", userAgent)
            .build()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(target)).await()
        if (response.statusCode() != 200) throw IOException("내려받지 못했습니다(HTTP ${response.statusCode()}).")

        return response.body()
    }

    private fun extractApp(dmg: Path, mountPoint: Path, target: Path) {
        Files.createDirectories(mountPoint)
        run("hdiutil", "attach", "-nobrowse", "-readonly", "-noautoopen", "-mountpoint", mountPoint.absolutePathString(), dmg.absolutePathString())
        try {
            val app = mountPoint.listDirectoryEntries("*.app").firstOrNull()
                ?: throw IOException("DMG 안에 앱이 없습니다.")
            run("ditto", app.absolutePathString(), target.absolutePathString())
        } finally {
            runCatching { run("hdiutil", "detach", "-force", mountPoint.absolutePathString()) }
        }
    }

    private fun launchReplacer(staged: Path, work: Path) {
        val script = work.resolve("update.sh")
        script.writeText(ReplaceScript)

        ProcessBuilder(
            "/bin/sh",
            script.absolutePathString(),
            ProcessHandle.current().pid().toString(),
            staged.absolutePathString(),
            bundle.absolutePathString(),
            work.absolutePathString(),
        )
            // 앱이 끝난 뒤에도 스크립트가 쓴다. 파이프로 두면 읽을 쪽이 사라져 SIGPIPE 로 죽는다.
            .redirectOutput(work.resolve("update.log").toFile())
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile()))
            .start()
    }

    private fun run(vararg command: String) {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        process.outputStream.close()
        val output = process.inputStream.readBytes().decodeToString()
        if (process.waitFor() != 0) throw IOException("${command.first()} 실패: ${output.trim()}")
    }
}

private fun sha256(file: Path): String =
    file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

@OptIn(kotlin.io.path.ExperimentalPathApi::class)
private fun Path.deleteRecursivelyIfExists() {
    if (exists()) deleteRecursively()
}

// $1 앱 pid, $2 풀어 둔 새 앱, $3 설치된 앱, $4 작업 폴더. 앱이 60초 안에 끝나지 않으면 바꾸지 않는다.
private val ReplaceScript: String = """
    pid="${'$'}1"; staged="${'$'}2"; target="${'$'}3"; work="${'$'}4"
    previous="${'$'}(dirname "${'$'}target")/.Jarvis-previous.app"
    waited=0
    while kill -0 "${'$'}pid" 2>/dev/null; do
      waited=${'$'}((waited + 1))
      if [ "${'$'}waited" -gt 600 ]; then rm -rf "${'$'}staged"; exit 1; fi
      sleep 0.1
    done
    rm -rf "${'$'}previous"
    if ! mv "${'$'}target" "${'$'}previous"; then rm -rf "${'$'}staged"; open "${'$'}target"; exit 1; fi
    if mv "${'$'}staged" "${'$'}target"; then
      rm -rf "${'$'}previous"
    else
      mv "${'$'}previous" "${'$'}target"
    fi
    open "${'$'}target"
    rm -rf "${'$'}work"
""".trimIndent() + "\n"
