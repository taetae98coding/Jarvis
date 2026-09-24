package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TerminalCommandTest {
    @Test
    fun shellIsALoginShell() {
        assertEquals(listOf("/bin/zsh", "-l"), terminalCommand(TerminalProgram.Shell, "/bin/zsh"))
    }

    @Test
    fun claudeRunsInYoloModeThenBecomesALoginShell() {
        assertEquals(
            listOf("/bin/zsh", "-l", "-i", "-c", "claude --dangerously-skip-permissions; exec '/bin/zsh' -l"),
            terminalCommand(TerminalProgram.Claude, "/bin/zsh"),
        )
    }

    @Test
    fun shellPathIsQuoted() {
        val command = terminalCommand(TerminalProgram.Claude, "/opt/it's here/zsh")

        assertEquals("claude --dangerously-skip-permissions; exec '/opt/it'\\''s here/zsh' -l", command.last())
    }

    @Test
    fun parentClaudeSessionIsNotInherited() {
        val environment = terminalEnvironment(
            mapOf(
                "CLAUDECODE" to "1",
                "CLAUDE_CODE_SESSION_ID" to "parent",
                "CLAUDE_CODE_USE_BEDROCK" to "1",
                "PATH" to "/usr/bin",
            ),
        )

        assertFalse("CLAUDECODE" in environment)
        assertFalse("CLAUDE_CODE_SESSION_ID" in environment)
        assertEquals("1", environment["CLAUDE_CODE_USE_BEDROCK"])
        assertEquals("/usr/bin", environment["PATH"])
    }
}
