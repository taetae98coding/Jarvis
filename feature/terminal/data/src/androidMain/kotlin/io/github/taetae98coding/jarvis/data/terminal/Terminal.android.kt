package io.github.taetae98coding.jarvis.data.terminal

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

// pty 를 열 공개 API 가 없어 파이프로 띄운다. 버린 후보(Termux JNI, /dev/ptmx)는
// docs/platform/android.html#terminal 에 있다.
internal actual fun createTerminalDataSource(context: PlatformContext): TerminalDataSource =
    PipeTerminalDataSource(home = context.context.filesDir)

private class PipeTerminalDataSource(
    private val home: File,
) : TerminalDataSource {
    override val isSupported: Boolean = true

    // Android 용 Claude Code CLI 가 없고, 있더라도 pty 없는 파이프에서는 TUI 가 그려지지 않는다.
    override val isClaudeSupported: Boolean = false

    override suspend fun open(size: TerminalSize, program: TerminalProgram): TerminalSession? {
        if (program != TerminalProgram.Shell) return null

        return openShell()
    }

    private suspend fun openShell(): TerminalSession? =
        withContext(Dispatchers.IO) {
            runCatching {
                // -i 가 없으면 표준 입력이 tty 가 아니라서 mksh 가 프롬프트를 내지 않는다. 프롬프트는
                // 표준 에러로 나오므로 합친다.
                val process = ProcessBuilder("/system/bin/sh", "-i")
                    .directory(home)
                    .redirectErrorStream(true)
                    .apply {
                        environment()["HOME"] = home.path
                        environment()["TERM"] = "dumb"
                    }
                    .start()

                PipeTerminalSession(process)
            }.getOrNull()
        }
}

private class PipeTerminalSession(
    private val process: Process,
) : TerminalSession {
    override val isPty: Boolean = false

    override val output: Flow<ByteArray> = flow {
        val input = process.inputStream
        val buffer = ByteArray(8 * 1024)

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) emit(buffer.copyOf(read))
        }
    }.catch {
        // close() 가 프로세스를 죽이면 읽던 스트림이 IOException 으로 끝난다.
    }.flowOn(Dispatchers.IO)

    override suspend fun write(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            runCatching {
                process.outputStream.write(bytes)
                process.outputStream.flush()
            }
        }
    }

    // 알릴 pty 가 없다.
    override fun resize(size: TerminalSize) = Unit

    override fun close() {
        process.destroy()
    }
}
