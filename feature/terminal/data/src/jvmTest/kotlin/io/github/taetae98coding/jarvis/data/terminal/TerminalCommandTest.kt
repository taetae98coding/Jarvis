package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalCommandTest {
    @Test
    fun shellIsALoginShell() {
        assertEquals(listOf("/bin/zsh", "-l"), terminalCommand("/bin/zsh"))
    }

    @Test
    fun interactiveCommandBecomesALoginShell() {
        assertEquals(
            listOf("/bin/zsh", "-l", "-i", "-c", "claude attach 'abc'; exec '/bin/zsh' -l"),
            interactiveCommand("/bin/zsh", claudeAttachScript("abc"), thenShell = true),
        )
    }

    @Test
    fun shellPathIsQuoted() {
        val command = interactiveCommand("/opt/it's here/zsh", "true", thenShell = true)

        assertEquals("true; exec '/opt/it'\\''s here/zsh' -l", command.last())
    }

    @Test
    fun newClaudeSessionIsNamedAfterThePaneSessionId() {
        assertEquals(
            "claude --bg --name 'jarvis-e0c0' --dangerously-skip-permissions",
            claudeStartScript("e0c0", resume = null),
        )
        assertEquals(
            "claude --bg --name 'jarvis-e0c0' --resume 'f1d1' --dangerously-skip-permissions",
            claudeStartScript("e0c0", resume = "f1d1"),
        )
    }

    @Test
    fun foregroundFallbackShowsWhyThenResumesOrStarts() {
        assertEquals(
            "printf '%s\\n\\n' 'Workspace not trusted.'; claude --dangerously-skip-permissions --session-id 'e0c0'",
            claudeForegroundScript("e0c0", resume = null, reason = "Workspace not trusted.\n"),
        )
        assertEquals(
            "claude --dangerously-skip-permissions --resume 'f1d1'",
            claudeForegroundScript("e0c0", resume = "f1d1", reason = ""),
        )
    }

    @Test
    fun backgroundJobsAreFoundBySessionIdAfterShellNoise() {
        val output = """
            zsh: some rc output
            [
              {"pid": 1, "cwd": "/a", "kind": "interactive", "sessionId": "interactive-id"},
              {"id": "de16776a", "cwd": "/b", "kind": "background", "sessionId": "de16776a-71a5", "name": "jarvis-s", "state": "stopped", "startedAt": 17}
            ]
        """.trimIndent()

        assertEquals(listOf(ClaudeJob("de16776a", "de16776a-71a5", "jarvis-s", "stopped", 17)), parseClaudeJobs(output))
    }

    @Test
    fun unreadableJobListIsEmpty() {
        assertTrue(parseClaudeJobs("command not found: claude").isEmpty())
        assertTrue(parseClaudeJobs("[ not json").isEmpty())
    }

    @Test
    fun backgroundedIdIsReadFromTheStartMessage() {
        assertEquals("39c21bb4", parseBackgroundedId("backgrounded · 39c21bb4 (idle — send a prompt to start)"))
        assertNull(parseBackgroundedId("Workspace not trusted."))
    }

    @Test
    fun existingBackgroundSessionIsAttachedWithoutStartingAnother() = runBlocking {
        val scripts = mutableListOf<String>()
        val claude = ClaudeBackground("/bin/zsh", run = { script, _ ->
            scripts += script
            ShellResult(0, """[{"id": "de16776a", "kind": "background", "sessionId": "s"}]""")
        })

        val command = claude.command("s", "/work")

        assertEquals(listOf("claude agents --json --all"), scripts)
        assertEquals("claude attach 'de16776a'; exec '/bin/zsh' -l", command.last())
    }

    @Test
    fun namedSessionIsAttachedEvenThoughClaudeChoseItsSessionId() = runBlocking {
        val claude = ClaudeBackground("/bin/zsh", run = { _, _ ->
            ShellResult(0, """[{"id": "7a7a7a7a", "kind": "background", "sessionId": "7a7a7a7a-0000", "name": "jarvis-s"}]""")
        })

        assertEquals("claude attach '7a7a7a7a'; exec '/bin/zsh' -l", claude.command("s", "/work").last())
    }

    @Test
    fun missingSessionIsStartedInThePaneDirectoryThenAttached() = runBlocking {
        val calls = mutableListOf<Pair<String, String>>()
        var started = false
        val claude = ClaudeBackground("/bin/zsh", run = { script, directory ->
            calls += script to directory
            when {
                script.startsWith("claude --bg") -> ShellResult(0, "backgrounded · 1234abcd").also { started = true }
                started -> ShellResult(0, """[{"id": "1234abcd", "kind": "background", "sessionId": "1234abcd-99", "name": "jarvis-s"}]""")
                else -> ShellResult(0, "[]")
            }
        }, hasConversation = { false })

        val command = claude.command("s", "/work")

        assertEquals(claudeStartScript("s", resume = null) to "/work", calls[1])
        assertEquals("claude attach '1234abcd'; exec '/bin/zsh' -l", command.last())
    }

    // 2.1.281 에서 처음 보는 uuid 로 --bg --resume 하면 세션이 failed 로 남는다. 그런 세션에 붙지 않는다.
    @Test
    fun failedSessionIsReplacedAndItsConversationResumed() = runBlocking {
        val scripts = mutableListOf<String>()
        val claude = ClaudeBackground("/bin/zsh", run = { script, _ ->
            scripts += script
            when {
                script.startsWith("claude --bg") -> ShellResult(0, "backgrounded · 1234abcd")
                else -> ShellResult(0, """[{"id": "dead", "kind": "background", "sessionId": "s", "state": "failed"}]""")
            }
        }, hasConversation = { it == "s" })

        val command = claude.command("s", "/work")

        assertEquals(claudeStartScript("s", resume = "s"), scripts[1])
        assertEquals("claude attach '1234abcd'; exec '/bin/zsh' -l", command.last())
    }

    @Test
    fun failedStartFallsBackToTheForeground() = runBlocking {
        val claude = ClaudeBackground("/bin/zsh", run = { script, _ ->
            if (script.startsWith("claude --bg")) ShellResult(1, "Workspace not trusted.") else ShellResult(0, "[]")
        }, hasConversation = { false })

        val command = claude.command("s", "/work")

        assertEquals(
            interactiveCommand("/bin/zsh", claudeForegroundScript("s", resume = null, reason = "Workspace not trusted."), thenShell = true),
            command,
        )
    }

    @Test
    fun stopFindsTheJobFirst() = runBlocking {
        val scripts = mutableListOf<String>()
        val claude = ClaudeBackground("/bin/zsh", run = { script, _ ->
            scripts += script
            ShellResult(0, """[{"id": "de16776a", "kind": "background", "sessionId": "s"}]""")
        })

        claude.stop("s")
        claude.stop("unknown")

        assertEquals(listOf("claude agents --json --all", "claude stop 'de16776a'", "claude agents --json --all"), scripts)
    }

    @Test
    fun lsofOutputGivesThePath() {
        assertEquals("/private/tmp", parseLsofDirectory("p86036\nfcwd\nn/private/tmp\n"))
        assertNull(parseLsofDirectory(""))
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
