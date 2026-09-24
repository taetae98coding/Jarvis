package io.github.taetae98coding.jarvis.data.terminal

import android.os.FileObserver
import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import okio.FileSystem
import okio.Path
import java.io.File

// 홈은 셸 탭과 같은 filesDir 이다(Terminal.android.kt).
internal actual fun createFileDataSource(context: PlatformContext): FileDataSource =
    OkioFileDataSource(
        fileSystem = FileSystem.SYSTEM,
        home = context.context.filesDir.path,
        dispatcher = Dispatchers.IO,
        changes = { path, isDirectory ->
            val file = path.toFile()
            if (isDirectory) {
                fileEvents(file, DirectoryEvents)
            } else {
                // 파일이 지워지면 그 inotify 감시도 사라진다. 다시 만들어진 것은 부모 폴더의 감시로 안다.
                merge(fileEvents(file, FileEvents), fileEvents(file.parentFile ?: file, DirectoryEvents))
            }
        },
    )

private fun fileEvents(file: File, mask: Int): Flow<Unit> =
    callbackFlow {
        val observer = object : FileObserver(file, mask) {
            override fun onEvent(event: Int, path: String?) {
                trySend(Unit)
            }
        }
        observer.startWatching()

        awaitClose { observer.stopWatching() }
    }

private fun Path.toFile(): File = File(toString())

private const val DirectoryEvents =
    FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_FROM or FileObserver.MOVED_TO or
        FileObserver.DELETE_SELF or FileObserver.MOVE_SELF or FileObserver.ATTRIB

private const val FileEvents =
    FileObserver.MODIFY or FileObserver.CLOSE_WRITE or FileObserver.ATTRIB or FileObserver.DELETE_SELF or FileObserver.MOVE_SELF
