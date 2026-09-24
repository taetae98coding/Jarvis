package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

internal class RecordingTerminalRepository(
    override val isSupported: Boolean = true,
    override val isClaudeSupported: Boolean = true,
    override val isBrowserSupported: Boolean = true,
) : TerminalRepository {
    val opened = mutableListOf<TerminalTab>()
    val stopped = mutableListOf<String>()

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession {
        opened += tab
        return NoopSession
    }

    override suspend fun stopClaude(sessionId: String) {
        stopped += sessionId
    }

    private object NoopSession : TerminalSession {
        override val isPty: Boolean = true
        override val output: Flow<ByteArray> = emptyFlow()

        override suspend fun write(bytes: ByteArray) = Unit

        override fun resize(size: TerminalSize) = Unit

        override fun close() = Unit
    }
}

internal class InMemoryTerminalWorkspaceRepository(
    initial: TerminalWorkspace = TerminalWorkspace.initial(),
) : TerminalWorkspaceRepository {
    val workspace = MutableStateFlow(initial)

    override fun observeWorkspace(): Flow<TerminalWorkspace> = workspace

    override suspend fun updateWorkspace(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange {
        val before = workspace.value
        val after = transform(before)
        workspace.value = after

        return TerminalWorkspaceChange(before, after)
    }
}
