package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenTerminalSessionUseCaseTest {
    private val claude = TerminalTab(1, TerminalProgram.Claude, claudeSessionId = "session")

    @Test
    fun opensTheRequestedPane() = runTest {
        val repository = RecordingTerminalRepository()

        OpenTerminalSessionUseCase(repository)(TerminalSize.Default, claude)

        assertEquals(listOf(claude), repository.opened)
    }

    @Test
    fun claudeIsNotOpenedWhereItIsNotSupported() = runTest {
        val repository = RecordingTerminalRepository(isClaudeSupported = false)

        val session = OpenTerminalSessionUseCase(repository)(TerminalSize.Default, claude)

        assertNull(session)
        assertEquals(emptyList(), repository.opened)
    }

    @Test
    fun claudeWithoutASessionIdIsNotOpened() = runTest {
        val repository = RecordingTerminalRepository()

        assertNull(OpenTerminalSessionUseCase(repository)(TerminalSize.Default, claude.copy(claudeSessionId = null)))
        assertEquals(emptyList(), repository.opened)
    }

    @Test
    fun browserTabsDoNotOpenASession() = runTest {
        val repository = RecordingTerminalRepository()
        val browser = TerminalTab(1, TerminalProgram.Browser, url = TerminalTab.DefaultBrowserUrl)

        assertNull(OpenTerminalSessionUseCase(repository)(TerminalSize.Default, browser))
        assertEquals(emptyList(), repository.opened)
    }

    @Test
    fun nothingIsOpenedWhereShellsAreNotSupported() = runTest {
        val repository = RecordingTerminalRepository(isSupported = false)

        assertNull(OpenTerminalSessionUseCase(repository)(TerminalSize.Default, TerminalTab(1)))
        assertEquals(emptyList(), repository.opened)
    }
}
