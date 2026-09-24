package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 실제 pty 로 셸을 띄운다. 로그인 셸이 아니라 /bin/sh 를 쓰는 이유는 개발자의 셸 설정이 결과를
 * 바꾸지 않게 하려는 것이다.
 */
class PtyTerminalDataSourceTest {
    private val dataSource = PtyTerminalDataSource(command = { listOf("/bin/sh") })

    private suspend fun TerminalSession.collectUntilExit(): String =
        output.fold("") { acc, bytes -> acc + bytes.decodeToString() }

    @Test
    fun runsCommandsAndEndsOnExit() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), TerminalProgram.Shell))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.write("echo jarvis-\$((40 + 2))\nexit\n".encodeToByteArray())

        assertTrue("jarvis-42" in text.await(), text.await())
    }

    @Test
    fun resizeReachesTheShell() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), TerminalProgram.Shell))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.resize(TerminalSize(columns = 123, rows = 45))
        session.write("stty size\nexit\n".encodeToByteArray())

        assertTrue("45 123" in text.await(), text.await())
    }

    @Test
    fun closeEndsTheOutput() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24), TerminalProgram.Shell))

        val done = async { withTimeout(10_000) { session.output.collect { } } }
        session.close()

        done.await()
    }

    // Claude 탭은 프로그램이 끝나면 exec 로 같은 pty 위의 셸이 이어받는다. 진짜 claude 대신 echo 를 쓴다.
    @Test
    fun programCommandRunsThenFallsBackToAShell() = runBlocking {
        val program = PtyTerminalDataSource(command = { listOf("/bin/sh", "-c", "echo from-program; exec '/bin/sh'") })
        val session = assertNotNull(program.open(TerminalSize(80, 24), TerminalProgram.Claude))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.write("echo from-shell-\$((40 + 2))\nexit\n".encodeToByteArray())

        assertTrue("from-program" in text.await(), text.await())
        assertTrue("from-shell-42" in text.await(), text.await())
    }

    @Test
    fun missingShellYieldsNull() = runBlocking {
        val missing = PtyTerminalDataSource(command = { listOf("/nonexistent/shell") })

        assertTrue(missing.open(TerminalSize(80, 24), TerminalProgram.Shell) == null)
    }
}
