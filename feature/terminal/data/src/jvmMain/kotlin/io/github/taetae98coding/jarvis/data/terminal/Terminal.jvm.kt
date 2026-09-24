package io.github.taetae98coding.jarvis.data.terminal

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

internal actual fun createTerminalDataSource(context: PlatformContext): TerminalDataSource =
    PtyTerminalDataSource(command = { program -> terminalCommand(program, loginShell()) })

internal class PtyTerminalDataSource(
    private val command: (TerminalProgram) -> List<String>,
) : TerminalDataSource {
    override val isSupported: Boolean = true

    // 설치 여부는 로그인 셸을 띄워 봐야 알 수 있어 미리 보지 않는다. 없으면 셸이 command not found 를 찍는다.
    override val isClaudeSupported: Boolean = true

    override suspend fun open(size: TerminalSize, program: TerminalProgram): TerminalSession? =
        withContext(Dispatchers.IO) {
            runCatching {
                val process = PtyProcessBuilder(command(program).toTypedArray())
                    .setEnvironment(terminalEnvironment(System.getenv()))
                    .setDirectory(System.getProperty("user.home"))
                    .setInitialColumns(size.columns)
                    .setInitialRows(size.rows)
                    .start()

                PtyTerminalSession(process)
            }.getOrNull()
        }
}

private class PtyTerminalSession(
    private val process: PtyProcess,
) : TerminalSession {
    override val isPty: Boolean = true

    // read 는 블록되고 코루틴 취소로 풀리지 않는다. close() 가 프로세스를 죽여야 스트림이 끝난다.
    override val output: Flow<ByteArray> = flow {
        val input = process.inputStream
        val buffer = ByteArray(ReadBufferSize)

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) emit(buffer.copyOf(read))
        }
    }.catch {
        // 셸이 끝나면 macOS 의 pty 마스터는 EOF 대신 EIO 를 준다. 끝난 것과 같게 다룬다.
    }.flowOn(Dispatchers.IO)

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

    override fun close() {
        process.destroy()
    }
}

private fun loginShell(): String = System.getenv("SHELL")?.takeIf { File(it).canExecute() } ?: DefaultShell

/**
 * 로그인 셸로 띄워야 `~/.zprofile` 의 PATH(Homebrew 등)가 들어온다. Finder 로 띄운 앱은 launchd 의
 * 최소 PATH 만 물려받는다.
 *
 * Claude 는 `-i` 까지 붙여 `~/.zshrc` 의 PATH 도 받는다(zsh 는 `-c` 면 비대화형이라 읽지 않는다). 끝나면
 * `exec` 가 같은 pty 를 로그인 셸로 바꿔서 패널이 닫히지 않는다.
 */
internal fun terminalCommand(program: TerminalProgram, shell: String): List<String> =
    when (program) {
        TerminalProgram.Shell -> listOf(shell, "-l")
        TerminalProgram.Claude -> listOf(shell, "-l", "-i", "-c", "$ClaudeYoloCommand; exec ${shellQuote(shell)} -l")
    }

private const val ClaudeYoloCommand = "claude --dangerously-skip-permissions"

private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

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
