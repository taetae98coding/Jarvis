package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenTerminalSessionUseCaseTest {
    @Test
    fun opensTheRequestedProgram() = runTest {
        val repository = RecordingTerminalRepository()

        OpenTerminalSessionUseCase(repository)(TerminalSize.Default, TerminalProgram.Claude)

        assertEquals(listOf(TerminalProgram.Claude), repository.opened)
    }

    @Test
    fun claudeIsNotOpenedWhereItIsNotSupported() = runTest {
        val repository = RecordingTerminalRepository(isClaudeSupported = false)

        val session = OpenTerminalSessionUseCase(repository)(TerminalSize.Default, TerminalProgram.Claude)

        assertNull(session)
        assertEquals(emptyList(), repository.opened)
    }

    @Test
    fun nothingIsOpenedWhereShellsAreNotSupported() = runTest {
        val repository = RecordingTerminalRepository(isSupported = false)

        assertNull(OpenTerminalSessionUseCase(repository)(TerminalSize.Default))
        assertEquals(emptyList(), repository.opened)
    }

    private class RecordingTerminalRepository(
        override val isSupported: Boolean = true,
        override val isClaudeSupported: Boolean = true,
    ) : TerminalRepository {
        val opened = mutableListOf<TerminalProgram>()

        override suspend fun open(size: TerminalSize, program: TerminalProgram): TerminalSession {
            opened += program
            return NoopSession
        }
    }

    private object NoopSession : TerminalSession {
        override val isPty: Boolean = true
        override val output: Flow<ByteArray> = emptyFlow()

        override suspend fun write(bytes: ByteArray) = Unit

        override fun resize(size: TerminalSize) = Unit

        override fun close() = Unit
    }
}
