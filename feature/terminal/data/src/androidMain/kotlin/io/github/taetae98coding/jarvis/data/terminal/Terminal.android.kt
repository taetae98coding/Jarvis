package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeStatus
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

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

    override val isBrowserSupported: Boolean = true

    // PC 에 로그인된 Chrome 의 데이터·키체인에 접근할 수 없다(다른 기기·다른 앱 샌드박스).
    override val isChromeImportSupported: Boolean = false

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession? {
        if (tab.program != TerminalProgram.Shell) return null

        return openShell(tab.directory?.let { File(expandHome(it, home.path)) }?.takeIf { it.isDirectory } ?: home)
    }

    override suspend fun stopClaude(sessionId: String) = Unit

    // Claude 탭이 없으니 알릴 턴도 없다. 알림 채널을 만들거나 권한을 묻지 않는다.
    override fun observeClaudeStatuses(): Flow<Map<String, ClaudeStatus>> = flowOf(emptyMap())

    override suspend fun showNotification(title: String, message: String) = Unit

    override fun observeChromeProfiles(): Flow<List<ChromeProfile>> = flowOf(emptyList())

    override suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie> = emptyList()

    private suspend fun openShell(directory: File): TerminalSession? =
        withContext(Dispatchers.IO) {
            runCatching {
                // -i 가 없으면 표준 입력이 tty 가 아니라서 mksh 가 프롬프트를 내지 않는다. 프롬프트는
                // 표준 에러로 나오므로 합친다. 첫 줄의 pid 는 작업 디렉터리를 /proc 에서 읽는 데 쓴다 —
                // exec 가 pid 를 물려주므로 대화형 셸의 pid 와 같다. Process.pid() 는 minSdk 에서 쓸 수 없다.
                val process = ProcessBuilder("/system/bin/sh", "-c", "echo \$\$; exec /system/bin/sh -i")
                    .directory(directory)
                    .redirectErrorStream(true)
                    .apply {
                        environment()["HOME"] = home.path
                        environment()["TERM"] = "dumb"
                    }
                    .start()
                val pid = process.inputStream.readLine().trim()

                PipeTerminalSession(process, DirectoryTracker { File("/proc/$pid/cwd").canonicalPath })
            }.getOrNull()
        }
}

// 버퍼를 두는 reader 로 읽으면 첫 줄 뒤의 셸 출력까지 삼킨다. 한 바이트씩 읽는다.
private fun InputStream.readLine(): String {
    val bytes = mutableListOf<Byte>()
    while (true) {
        val byte = read()
        if (byte < 0 || byte == '\n'.code) break
        bytes += byte.toByte()
    }
    return bytes.toByteArray().decodeToString()
}

private class PipeTerminalSession(
    private val process: Process,
    private val tracker: DirectoryTracker,
) : TerminalSession {
    override val isPty: Boolean = false

    override val output: Flow<ByteArray> = flow {
        val input = process.inputStream
        val buffer = ByteArray(8 * 1024)

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) {
                emit(buffer.copyOf(read))
                tracker.onOutput()
            }
        }
    }.catch {
        // close() 가 프로세스를 죽이면 읽던 스트림이 IOException 으로 끝난다.
    }.flowOn(Dispatchers.IO)

    override val directory: Flow<String> = tracker.directory.flowOn(Dispatchers.IO)

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
