package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TypedCommandSessionTest {
    private class FakeSession : TerminalSession {
        val chunks = Channel<ByteArray>(Channel.UNLIMITED)
        val written = mutableListOf<String>()
        override val isPty: Boolean = true
        override val output: Flow<ByteArray> = chunks.consumeAsFlow()
        override suspend fun write(bytes: ByteArray) {
            written += bytes.decodeToString()
        }
        override fun resize(size: TerminalSize) = Unit
        override fun close() = Unit
    }

    private fun FakeSession.emit(text: String) = chunks.trySend(text.encodeToByteArray())

    @Test
    fun typesTheCommandOnceTheShellTurnsOnBracketedPaste() = runTest {
        val shell = FakeSession()
        val session = TypedCommandSession(shell, "make test", readyTimeout = 3.seconds)
        val seen = mutableListOf<String>()
        val collector = launch { session.output.collect { seen += it.decodeToString() } }

        shell.emit("Last login\r\n")
        runCurrent()
        assertEquals(emptyList(), shell.written)

        shell.emit("user@mac ~ % \u001b[?2004h")
        runCurrent()
        assertEquals(listOf("\u001b[200~make test\u001b[201~\r"), shell.written)

        shell.emit("\u001b[?2004l\r\n\u001b[?2004h")
        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(1, shell.written.size)
        assertEquals(listOf("Last login\r\n", "user@mac ~ % \u001b[?2004h", "\u001b[?2004l\r\n\u001b[?2004h"), seen)

        shell.chunks.close()
        collector.join()
    }

    @Test
    fun aSignalSplitAcrossChunksStillCounts() = runTest {
        val shell = FakeSession()
        val session = TypedCommandSession(shell, "make", readyTimeout = 3.seconds)
        val collector = launch { session.output.collect {} }

        shell.emit("% \u001b[?20")
        runCurrent()
        assertEquals(emptyList(), shell.written)
        shell.emit("04h")
        runCurrent()
        assertEquals(listOf("\u001b[200~make\u001b[201~\r"), shell.written)

        shell.chunks.close()
        collector.join()
    }

    @Test
    fun withoutASignalTheCommandIsTypedPlainAfterTheTimeout() = runTest {
        val shell = FakeSession()
        val session = TypedCommandSession(shell, "make", readyTimeout = 3.seconds)
        val collector = launch { session.output.collect {} }

        shell.emit("bash-3.2$ ")
        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(emptyList(), shell.written)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(listOf("make\r"), shell.written)

        shell.chunks.close()
        collector.join()
    }

    @Test
    fun outputEndingBeforeTheTimeoutStopsTheTimer() = runTest {
        val shell = FakeSession()
        val session = TypedCommandSession(shell, "make", readyTimeout = 3.seconds)

        shell.emit("bye\r\n")
        shell.chunks.close()
        assertEquals(listOf("bye\r\n"), session.output.toList().map { it.decodeToString() })
        advanceTimeBy(10.seconds)
        runCurrent()
        assertTrue(shell.written.isEmpty())
    }

    @Test
    fun sequenceMatcherRestartsOnAFalseStart() {
        val matcher = SequenceMatcher("abc".encodeToByteArray())
        assertFalse(matcher.feed("ab".encodeToByteArray()))
        assertFalse(matcher.feed("ab".encodeToByteArray()))
        assertTrue(matcher.feed("c".encodeToByteArray()))
        assertTrue(matcher.feed("zzz".encodeToByteArray()))
    }
}
