package io.github.taetae98coding.jarvis.data.terminal

import androidx.datastore.core.DataStore
import io.github.taetae98coding.jarvis.data.PlatformContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath

internal actual fun createTerminalWorkspaceStore(context: PlatformContext): DataStore<TerminalWorkspaceDto>? =
    terminalWorkspaceStore(
        fileSystem = FileSystem.SYSTEM,
        path = context.context.filesDir.resolve("datastore").resolve(TerminalWorkspaceFileName).toOkioPath(),
    )
