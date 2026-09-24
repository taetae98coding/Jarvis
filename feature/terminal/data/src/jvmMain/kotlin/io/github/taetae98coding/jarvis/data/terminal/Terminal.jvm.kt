package io.github.taetae98coding.jarvis.data.terminal

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

internal actual fun createTerminalDataSource(context: PlatformContext): TerminalDataSource {
    val shell = loginShell()
    val claude = ClaudeBackground(shell)

    return PtyTerminalDataSource(
        launch = { tab ->
            val directory = startDirectory(tab)
            when (tab.program) {
                TerminalProgram.Shell -> PtyLaunch(terminalCommand(shell), directory, tracksDirectory = true)
                TerminalProgram.Claude -> PtyLaunch(claude.command(checkNotNull(tab.claudeSessionId), directory), directory)
                TerminalProgram.Browser, TerminalProgram.Device -> error("브라우저·기기 탭은 세션을 열지 않는다")
            }
        },
        claude = claude,
    )
}

/** pty 에 띄울 명령. [tracksDirectory] 면 셸의 작업 디렉터리를 [TerminalSession.directory] 로 흘린다. */
internal class PtyLaunch(
    val command: List<String>,
    val directory: String,
    val tracksDirectory: Boolean = false,
)

internal class PtyTerminalDataSource(
    private val launch: suspend (TerminalTab) -> PtyLaunch,
    private val claude: ClaudeBackground? = null,
    private val readDirectory: (Long) -> String? = ::processDirectory,
) : TerminalDataSource {
    override val isSupported: Boolean = true

    // 설치 여부는 로그인 셸을 띄워 봐야 알 수 있어 미리 보지 않는다. 없으면 셸이 command not found 를 찍는다.
    override val isClaudeSupported: Boolean = true

    // 데스크탑은 macOS 만 지원한다. 다른 OS 에서는 웹뷰 네이티브를 불러 보지도 않고 메뉴 항목을 뺀다.
    override val isBrowserSupported: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

    // macOS 이고 Chrome 이 깔려 있을 때만. 읽기·복호화는 ChromeCookieReader 가 한다.
    override val isChromeImportSupported: Boolean = ChromeCookieReader.isSupported

    override fun observeChromeProfiles(): Flow<List<ChromeProfile>> = ChromeCookieReader.observeProfiles()

    override suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie> =
        ChromeCookieReader.importCookies(profileDirectory)

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession? =
        withContext(Dispatchers.IO) {
            runCatching {
                val launch = launch(tab)
                val process = PtyProcessBuilder(launch.command.toTypedArray())
                    .setEnvironment(terminalEnvironment(System.getenv()))
                    .setDirectory(launch.directory)
                    .setInitialColumns(size.columns)
                    .setInitialRows(size.rows)
                    .start()

                val tracker = if (launch.tracksDirectory) DirectoryTracker { readDirectory(process.pid()) } else null
                PtyTerminalSession(process, tracker)
            }.getOrNull()
        }

    override suspend fun stopClaude(sessionId: String) {
        claude?.stop(sessionId)
    }
}

private class PtyTerminalSession(
    private val process: PtyProcess,
    private val tracker: DirectoryTracker?,
) : TerminalSession {
    override val isPty: Boolean = true

    // read 는 블록되고 코루틴 취소로 풀리지 않는다. close() 가 프로세스를 죽여야 스트림이 끝난다.
    override val output: Flow<ByteArray> = flow {
        val input = process.inputStream
        val buffer = ByteArray(ReadBufferSize)

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) {
                emit(buffer.copyOf(read))
                tracker?.onOutput()
            }
        }
    }.catch {
        // 셸이 끝나면 macOS 의 pty 마스터는 EOF 대신 EIO 를 준다. 끝난 것과 같게 다룬다.
    }.flowOn(Dispatchers.IO)

    override val directory: Flow<String> = tracker?.directory?.flowOn(Dispatchers.IO) ?: emptyFlow()

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            runCatching {
                process.outputStream.write(bytes)
                process.outputStream.flush()
            }
        }
    }

    override fun resize(size: TerminalSize) {
        runCatching { process.winSize = WinSize(size.columns, size.rows) }
    }

    // destroy() 는 SIGTERM 만 보내는데 대화형 셸은 SIGTERM 을 무시한다(zsh·sh 로 확인). 창을 닫은 터미널처럼
    // pty 마스터를 닫으면 커널이 셸에 SIGHUP 을 보내고, 셸은 자기 작업들에 SIGHUP 을 넘기고 끝난다.
    override fun close() {
        runCatching { process.inputStream.close() }
        process.destroy()
    }
}

private fun loginShell(): String = System.getenv("SHELL")?.takeIf { File(it).canExecute() } ?: DefaultShell

/** 저장된 작업 디렉터리가 지금도 있으면 거기서, 아니면 홈에서 시작한다. */
private fun startDirectory(tab: TerminalTab): String {
    val home = System.getProperty("user.home")

    return tab.directory?.let { expandHome(it, home) }?.takeIf { File(it).isDirectory } ?: home
}

/**
 * 로그인 셸로 띄워야 `~/.zprofile` 의 PATH(Homebrew 등)가 들어온다. Finder 로 띄운 앱은 launchd 의
 * 최소 PATH 만 물려받는다.
 */
internal fun terminalCommand(shell: String): List<String> = listOf(shell, "-l")

/**
 * [script] 를 셸 설정이 적용된 셸에서 돌린다. zsh 는 `-c` 면 비대화형이라 `~/.zshrc` 를 읽지 않아서
 * `-i` 를 붙인다 — `claude` 의 PATH 를 `~/.zshrc` 에 적는 설치가 많다. 끝나면 `exec` 가 같은 pty 를
 * 로그인 셸로 바꿔서 창이 닫히지 않는다.
 */
internal fun interactiveCommand(shell: String, script: String, thenShell: Boolean): List<String> {
    val line = if (thenShell) "$script; exec ${shellQuote(shell)} -l" else script
    return listOf(shell, "-l", "-i", "-c", line)
}

internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

/** 출력은 `p<pid>`, `fcwd`, `n<경로>` 줄이다. */
internal fun parseLsofDirectory(output: String): String? =
    output.lineSequence().firstOrNull { it.startsWith("n") }?.removePrefix("n")?.takeIf { it.isNotEmpty() }

// macOS 에는 /proc 이 없다. JNA 로 proc_pidinfo 를 부르는 안을 버린 이유는 docs/platform/jvm.html#terminal-panels 에 있다.
private fun processDirectory(pid: Long): String? {
    val process = ProcessBuilder("lsof", "-a", "-p", pid.toString(), "-d", "cwd", "-Fn")
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()

    return parseLsofDirectory(output)
}

// 데스크탑은 macOS 만 지원한다. macOS 10.15 부터 기본 셸이 zsh 다. 다른 OS 에서 없으면 실행이 실패하고
// open() 이 null 이 된다.
private const val DefaultShell = "/bin/zsh"

private const val ReadBufferSize = 8 * 1024

internal fun terminalEnvironment(inherited: Map<String, String>): Map<String, String> =
    inherited.toMutableMap().apply {
        // 앱을 Claude Code 세션 안에서 띄웠으면(개발 중 gradle run) 그 세션의 변수가 남는다. CLAUDECODE 가
        // 있으면 claude 가 중첩 세션이라며 뜨지 않는다. 이름은 Claude Code 2.1.281 에서 본 것이다.
        keys -= ParentClaudeSessionVariables
        put("TERM", "xterm-256color")
        put("COLORTERM", "truecolor")
        // Finder 로 띄운 앱에는 LANG 이 없다. 그러면 zsh 가 한글을 바이트 단위로 지우고 자른다.
        putIfAbsent("LANG", "en_US.UTF-8")
    }

private val ParentClaudeSessionVariables = setOf(
    "CLAUDECODE",
    "CLAUDE_PID",
    "CLAUDE_CODE_ENTRYPOINT",
    "CLAUDE_CODE_EXECPATH",
    "CLAUDE_CODE_SESSION_ID",
    "CLAUDE_CODE_CHILD_SESSION",
    "CLAUDE_CODE_SESSION_ATTENDED",
    "CLAUDE_CODE_MESSAGING_SOCKET",
    "CLAUDE_CODE_MESSAGING_TOKEN",
)
