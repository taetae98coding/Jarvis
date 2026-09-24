package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 실제 pty 로 셸을 띄운다. 로그인 셸이 아니라 /bin/sh 를 쓰는 이유는 개발자의 셸 설정이 결과를
 * 바꾸지 않게 하려는 것이다.
 */
class PtyTerminalDataSourceTest {
    private val home = System.getProperty("user.home")

    private val dataSource = PtyTerminalDataSource(
        launch = { pane -> PtyLaunch(listOf("/bin/sh"), pane.directory ?: home, tracksDirectory = true) },
    )

    private val shell = TerminalTab(1)

    private suspend fun TerminalSession.collectUntilExit(): String =
        output.fold("") { acc, bytes -> acc + bytes.decodeToString() }

    @Test
    fun runsCommandsAndEndsOnExit() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), shell))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.write("echo jarvis-\$((40 + 2))\nexit\n".encodeToByteArray())

        assertTrue("jarvis-42" in text.await(), text.await())
    }

    @Test
    fun resizeReachesTheShell() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), shell))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.resize(TerminalSize(columns = 123, rows = 45))
        session.write("stty size\nexit\n".encodeToByteArray())

        assertTrue("45 123" in text.await(), text.await())
    }

    @Test
    fun closeEndsTheOutput() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), shell))

        val done = async { withTimeout(10_000) { session.output.collect { } } }
        session.close()

        done.await()
    }

    @Test
    fun shellStartsInTheSavedDirectory() = runBlocking {
        val directory = realDirectory()
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), shell.copy(directory = directory)))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.write("pwd -P\nexit\n".encodeToByteArray())

        assertTrue(directory in text.await(), text.await())
    }

    // 작업 디렉터리는 셸 출력이 멎을 때 lsof 로 읽는다. cd 뒤 프롬프트가 그려지면 새 경로가 온다.
    @Test
    fun directoryFollowsCd() = runBlocking {
        val target = realDirectory()
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), shell))
        val reader = launch { session.output.collect { } }

        try {
            val changed = async { withTimeout(10_000) { session.directory.first { it == target } } }
            session.write("cd '$target'\n".encodeToByteArray())

            assertEquals(target, changed.await())
        } finally {
            // 읽기는 블록돼 있어 취소로 풀리지 않는다. 셸을 죽여야 runBlocking 이 끝난다.
            session.close()
            reader.cancel()
        }
    }

    // Claude 창은 프로그램이 끝나면 exec 로 같은 pty 위의 셸이 이어받는다. 진짜 claude 대신 echo 를 쓴다.
    @Test
    fun programCommandRunsThenFallsBackToAShell() = runBlocking {
        val program = PtyTerminalDataSource(
            launch = { PtyLaunch(interactiveCommand("/bin/sh", "echo from-program", thenShell = true), home) },
        )
        val session = assertNotNull(program.open(TerminalSize(80, 24), shell))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.write("echo from-shell-\$((40 + 2))\nexit\n".encodeToByteArray())

        assertTrue("from-program" in text.await(), text.await())
        assertTrue("from-shell-42" in text.await(), text.await())
    }

    @Test
    fun missingShellYieldsNull() = runBlocking {
        val missing = PtyTerminalDataSource(launch = { PtyLaunch(listOf("/nonexistent/shell"), home) })

        assertTrue(missing.open(TerminalSize(80, 24), shell) == null)
    }

    // macOS 의 임시 디렉터리는 /var → /private/var 심볼릭 링크를 거친다. 셸과 lsof 는 실제 경로를 준다.
    private fun realDirectory(): String =
        File(Files.createTempDirectory("jarvis-pty").toFile().canonicalPath).also { it.deleteOnExit() }.path
}
