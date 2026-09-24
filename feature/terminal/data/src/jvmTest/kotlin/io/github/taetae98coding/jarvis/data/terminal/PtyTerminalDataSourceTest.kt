package io.github.taetae98coding.jarvis.data.terminal

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
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24)))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.write("echo jarvis-\$((40 + 2))\nexit\n".encodeToByteArray())

        assertTrue("jarvis-42" in text.await(), text.await())
    }

    @Test
    fun resizeReachesTheShell() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24)))

        val text = async { withTimeout(10_000) { session.collectUntilExit() } }
        session.resize(TerminalSize(columns = 123, rows = 45))
        session.write("stty size\nexit\n".encodeToByteArray())

        assertTrue("45 123" in text.await(), text.await())
    }

    @Test
    fun closeEndsTheOutput() = runBlocking {
        val session = assertNotNull(dataSource.open(TerminalSize(80, 24)))

        val done = async { withTimeout(10_000) { session.output.collect { } } }
        session.close()

        done.await()
    }

    @Test
    fun missingShellYieldsNull() = runBlocking {
        val missing = PtyTerminalDataSource(command = { listOf("/nonexistent/shell") })

        assertTrue(missing.open(TerminalSize(80, 24)) == null)
    }
}
