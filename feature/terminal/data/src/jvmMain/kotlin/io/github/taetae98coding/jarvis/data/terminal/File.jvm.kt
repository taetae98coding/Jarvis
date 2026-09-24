package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// macOS 의 JDK WatchService 는 PollingWatchService 라 콜백이 없다(docs/platform/jvm.html#terminal-side-bar).
internal actual fun createFileDataSource(context: PlatformContext): FileDataSource =
    OkioFileDataSource(
        fileSystem = FileSystem.SYSTEM,
        home = System.getProperty("user.home"),
        dispatcher = Dispatchers.IO,
        changes = { _, _ -> pollingTicks(FilePollInterval) },
    )

internal val FilePollInterval: Duration = 2.seconds
