package io.github.taetae98coding.jarvis.data.terminal

import androidx.datastore.core.DataStore
import io.github.taetae98coding.jarvis.data.PlatformContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

// 데스크탑은 macOS 만 지원한다. 다른 OS 에서도 같은 경로에 쓴다.
internal actual fun createTerminalWorkspaceStore(context: PlatformContext): DataStore<TerminalWorkspaceDto>? =
    terminalWorkspaceStore(
        fileSystem = FileSystem.SYSTEM,
        path = File(System.getProperty("user.home"), "Library/Application Support/Jarvis/$TerminalWorkspaceFileName").toOkioPath(),
    )
