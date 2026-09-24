package io.github.taetae98coding.jarvis.data.terminal

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.github.taetae98coding.jarvis.data.PlatformContext
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
    PtyTerminalDataSource(command = ::loginShellCommand)

internal class PtyTerminalDataSource(
    private val command: () -> List<String>,
) : TerminalDataSource {
    override val isSupported: Boolean = true

    override suspend fun open(size: TerminalSize): TerminalSession? =
        withContext(Dispatchers.IO) {
            runCatching {
                val process = PtyProcessBuilder(command().toTypedArray())
                    .setEnvironment(terminalEnvironment())
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

/**
 * 로그인 셸로 띄워야 `~/.zprofile` 의 PATH(Homebrew 등)가 들어온다. Finder 로 띄운 앱은 launchd 의
 * 최소 PATH 만 물려받는다.
 */
private fun loginShellCommand(): List<String> {
    val shell = System.getenv("SHELL")?.takeIf { File(it).canExecute() } ?: DefaultShell

    return listOf(shell, "-l")
}

// 데스크탑은 macOS 만 지원한다. macOS 10.15 부터 기본 셸이 zsh 다. 다른 OS 에서 없으면 실행이 실패하고
// open() 이 null 이 된다.
private const val DefaultShell = "/bin/zsh"

private const val ReadBufferSize = 8 * 1024

private fun terminalEnvironment(): Map<String, String> =
    System.getenv().toMutableMap().apply {
        put("TERM", "xterm-256color")
        put("COLORTERM", "truecolor")
        // Finder 로 띄운 앱에는 LANG 이 없다. 그러면 zsh 가 한글을 바이트 단위로 지우고 자른다.
        putIfAbsent("LANG", "en_US.UTF-8")
    }
