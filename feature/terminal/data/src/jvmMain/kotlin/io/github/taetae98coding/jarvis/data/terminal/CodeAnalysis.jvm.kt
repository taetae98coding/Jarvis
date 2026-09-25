package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeAnalysisStatus
import io.github.taetae98coding.jarvis.domain.terminal.CodeCompletion
import io.github.taetae98coding.jarvis.domain.terminal.CodeEdit
import io.github.taetae98coding.jarvis.domain.terminal.CodeIntelRepository
import io.github.taetae98coding.jarvis.domain.terminal.CodeLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal actual fun createCodeIntelRepository(): CodeIntelRepository =
    DefaultCodeIntelRepository(LspCodeAnalysisDataSource(LspServerLauncher()), TextCodeSearch(FileSystem.SYSTEM, Dispatchers.IO))

/**
 * (언어, 루트)마다 언어 서버 하나를 나눠 쓴다(N1·N2). 마지막 수집이 끝나고 [idleTimeout] 이 지나면 서버를 끈다 — Kotlin 서버의
 * 임포트가 수십 초라 탭을 오갈 때마다 다시 띄우지 않게(docs/common/terminal-code-navigation.html 의 결정).
 */
internal class LspCodeAnalysisDataSource(
    private val launcher: LspLauncher,
    private val idleTimeout: Duration = CodeAnalysisIdleTimeout,
) : CodeAnalysisDataSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val shared = ConcurrentHashMap<SessionKey, Flow<LaunchResult>>()
    private val running = ConcurrentHashMap<SessionKey, LspSession>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(language: CodeLanguage, root: String, path: String): Flow<CodeAnalysisStatus> =
        session(SessionKey(language, root)).flatMapLatest { result ->
            when (result) {
                is LaunchResult.Unavailable -> flowOf(CodeAnalysisStatus.Unavailable(result.reason))
                is LaunchResult.Failed -> flowOf(CodeAnalysisStatus.Failed(result.reason))
                is LaunchResult.Running -> result.session.status.onCompletion { result.session.close(path) }
            }
        }

    override suspend fun complete(language: CodeLanguage, root: String, path: String, text: String, offset: Int): List<CodeCompletion>? =
        running[SessionKey(language, root)]?.complete(path, text, offset)

    override suspend fun applyCompletion(language: CodeLanguage, root: String, path: String, text: String, offset: Int, item: CodeCompletion): CodeEdit? =
        running[SessionKey(language, root)]?.applyCompletion(path, text, offset, item)

    override suspend fun definition(language: CodeLanguage, root: String, path: String, text: String, offset: Int): AnalysisLocations? =
        running[SessionKey(language, root)]?.definition(path, text, offset)

    override suspend fun usages(language: CodeLanguage, root: String, path: String, text: String, offset: Int): AnalysisLocations? =
        running[SessionKey(language, root)]?.usages(path, text, offset)

    private fun session(key: SessionKey): Flow<LaunchResult> =
        shared.getOrPut(key) {
            flow {
                val result = launcher.launch(key.language, key.root)
                if (result !is LaunchResult.Running) {
                    emit(result)
                    awaitCancellation()
                }
                val session = result.session
                val watcher = scope.launch {
                    session.closed.await()
                    session.onClosed()
                    running.remove(key, session)
                }
                try {
                    if (session.start()) running[key] = session
                    emit(result)
                    awaitCancellation()
                } finally {
                    running.remove(key, session)
                    watcher.cancel()
                    withContext(NonCancellable) { result.stop() }
                }
            }
                .flowOn(Dispatchers.IO)
                .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = idleTimeout.inWholeMilliseconds, replayExpirationMillis = 0), replay = 1)
        }

    private data class SessionKey(val language: CodeLanguage, val root: String)
}

internal sealed interface LaunchResult {
    data class Unavailable(val reason: String) : LaunchResult

    data class Failed(val reason: String) : LaunchResult

    /** [stop] 은 세션을 닫고 서버 프로세스를 정리한다. */
    class Running(val session: LspSession, val stop: suspend () -> Unit) : LaunchResult
}

internal fun interface LspLauncher {
    fun launch(language: CodeLanguage, root: String): LaunchResult
}

/** `kotlin-lsp`·`xcrun sourcekit-lsp` 를 찾아 stdio 로 띄운다(docs/platform/jvm.html#terminal-code-navigation). */
internal class LspServerLauncher : LspLauncher {
    private val processes = ConcurrentHashMap.newKeySet<Process>()

    init {
        // JVM 이 끝나도 자식 프로세스는 남는다. 앱을 끄면 서버도 끈다(N2).
        Runtime.getRuntime().addShutdownHook(Thread { processes.forEach(::destroyTree) })
    }

    override fun launch(language: CodeLanguage, root: String): LaunchResult {
        val environment = serverEnvironment()
        val command = when (language) {
            CodeLanguage.Kotlin -> {
                val server = kotlinServer(environment)
                    ?: return LaunchResult.Unavailable("kotlin-lsp 를 찾지 못했습니다 — brew install --cask kotlin-lsp")
                listOf(server.path, "--stdio", "--system-path=${cacheDirectory("kotlin-lsp").path}") to environment
            }
            CodeLanguage.Swift -> {
                val swift = swiftServer(File(root), environment)
                    ?: return LaunchResult.Unavailable("sourcekit-lsp 를 찾지 못했습니다 — Xcode 나 Command Line Tools 를 설치하세요")
                swift
            }
        }

        val errors = File.createTempFile("jarvis-lsp", ".err")
        val process = runCatching {
            ProcessBuilder(command.first)
                .directory(File(root))
                .redirectError(errors)
                .apply {
                    environment().clear()
                    environment().putAll(command.second)
                }
                .start()
        }.getOrElse {
            errors.delete()
            return LaunchResult.Failed(it.message ?: "분석 서버를 띄우지 못했습니다")
        }
        processes += process
        val session = LspSession(language, root, process.inputStream, process.outputStream, readyOnInitialize = language == CodeLanguage.Swift)
        process.onExit().thenRun {
            val last = runCatching { errors.readLines().lastOrNull { it.isNotBlank() } }.getOrNull()
            if (process.exitValue() != 0 && last != null) session.fail(last.take(ErrorMessageMaxLength))
        }

        return LaunchResult.Running(session) {
            // 런처가 exit 로 끝나면 자식 JVM 은 부모를 잃어 더 찾을 수 없다. 닫기 전에 먼저 적어 둔다.
            val tree = process.descendants().toList()
            session.shutdown()
            if (!process.waitFor(StopGrace.inWholeMilliseconds, TimeUnit.MILLISECONDS)) process.destroyForcibly()
            tree.filter { it.isAlive }.forEach { it.destroyForcibly() }
            processes -= process
            errors.delete()
        }
    }

    private fun kotlinServer(environment: Map<String, String>): File? {
        val found = executableOnPath("kotlin-lsp", environment)?.canonicalFile ?: return null
        // cask 가 링크하는 kotlin-lsp.sh 는 폐기 예정 경고를 쓰고 옆의 bin/intellij-server 를 부른다.
        val server = File(found.parentFile, "bin/intellij-server")

        return if (found.name == "kotlin-lsp.sh" && server.canExecute()) server else found
    }

    private fun swiftServer(root: File, base: Map<String, String>): Pair<List<String>, Map<String, String>>? {
        val xcrun = File("/usr/bin/xcrun").takeIf { it.canExecute() } ?: return null
        val database = if (File(root, "Package.swift").isFile) null else swiftCompileDatabase(root, cacheDirectory("sourcekit-lsp"), base)
        val environment = database?.environment ?: base
        if (runQuietly(listOf(xcrun.path, "--find", "sourcekit-lsp"), environment) == null) return null

        val arguments = database?.let { listOf("--compilation-db-search-path", it.searchPath) }.orEmpty()

        return listOf(xcrun.path, "sourcekit-lsp") + arguments to environment
    }

    private fun cacheDirectory(name: String): File =
        File(System.getProperty("user.home"), "Library/Caches/Jarvis/$name").apply { mkdirs() }

    private companion object {
        val StopGrace = 2.seconds

        const val ErrorMessageMaxLength = 160
    }
}

internal val CodeAnalysisIdleTimeout: Duration = 60.seconds

// kotlin-lsp 의 bin/intellij-server 는 네이티브 런처라 JVM 을 자식 프로세스로 띄운다(2026-09-25 확인). 런처만 끄면 JVM 이
// 부모 없이 남는다.
private fun destroyTree(process: Process) {
    process.descendants().forEach { it.destroyForcibly() }
    process.destroyForcibly()
}

private fun executableOnPath(name: String, environment: Map<String, String>): File? {
    val home = System.getProperty("user.home")
    val directories = environment["PATH"].orEmpty().split(':').filter { it.isNotBlank() } +
        listOf("/opt/homebrew/bin", "/usr/local/bin", "$home/.local/bin")

    return directories.asSequence().map { File(it, name) }.firstOrNull { it.canExecute() }
}

/** 로그인 셸의 환경에 Gradle 임포트가 쓰는 `ANDROID_HOME` 을 채운다. */
private fun serverEnvironment(): Map<String, String> =
    LoginShellEnvironment.value.toMutableMap().apply {
        if (get("ANDROID_HOME").isNullOrBlank()) androidSdkDirectory()?.let { put("ANDROID_HOME", it.path) }
    }
