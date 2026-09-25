package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeOnSignals
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.time.Duration

internal interface FileDataSource {
    fun observeDirectory(directory: String): Flow<List<FileEntry>?>

    fun observeFile(path: String): Flow<FileContent>

    suspend fun writeFile(path: String, text: String): Result<Unit>
}

/** 파일을 볼 수 없는 타깃(터미널 화면에 들어갈 수 없는 iOS·Web). */
internal object UnsupportedFileDataSource : FileDataSource {
    override fun observeDirectory(directory: String): Flow<List<FileEntry>?> = flowOf(null)

    override fun observeFile(path: String): Flow<FileContent> = flowOf(FileContent.Unreadable)

    override suspend fun writeFile(path: String, text: String): Result<Unit> = Result.failure(UnsupportedOperationException("파일을 쓸 수 없는 플랫폼입니다"))
}

/** 판정 근거는 docs/common/terminal-side-bar.html#platforms 에 있다. */
internal expect fun createFileDataSource(context: PlatformContext): FileDataSource

/**
 * okio [FileSystem] 으로 폴더 목록과 파일 앞부분을 읽는다. 언제 다시 읽을지는 플랫폼이 [changes] 로 알린다 —
 * Android 는 inotify 콜백, JVM(macOS)은 폴링이다(docs/platform/jvm.html#terminal-side-bar).
 * [changes] 의 [Path] 는 `~` 를 편 경로이고, `isDirectory` 는 폴더 목록을 보려는 것인지다.
 */
internal class OkioFileDataSource(
    private val fileSystem: FileSystem,
    private val home: String,
    private val dispatcher: CoroutineDispatcher,
    private val changes: (path: Path, isDirectory: Boolean) -> Flow<Unit>,
) : FileDataSource {
    // 쓴 경로. 폴링 간격을 기다리지 않고 그 파일을 보고 있는 observeFile 이 곧장 다시 읽게 한다(docs/common/terminal-file-editor.html E8).
    private val written = MutableSharedFlow<Path>(extraBufferCapacity = 16)

    override fun observeDirectory(directory: String): Flow<List<FileEntry>?> {
        val path = expandHome(directory, home).toPath()

        return observeOnSignals(changes(path, true)) { list(path) }.flowOn(dispatcher)
    }

    // 신호마다 크기·수정 시각만 보고, 달라졌을 때만 내용을 읽는다. 폴링 틱마다 512 KiB 를 읽지 않게.
    override fun observeFile(path: String): Flow<FileContent> {
        val file = expandHome(path, home).toPath()

        val signals = merge(changes(file, false), written.filter { it == file }.map { })

        return observeOnSignals(signals) { stamp(file) }
            .map { stamp -> if (stamp == null) FileContent.Unreadable else read(file) }
            // 쓰는 도중(크기만 바뀌고 시각은 그대로)에 한 번, 다 쓴 뒤에 한 번 읽으면 같은 내용이 두 번 나온다.
            .distinctUntilChanged()
            .flowOn(dispatcher)
    }

    // 임시 파일을 옮겨 놓지 않고 그 파일에 바로 쓴다. 심볼릭 링크·권한·inode 를 그대로 둔다(공통 스펙의 결정).
    override suspend fun writeFile(path: String, text: String): Result<Unit> {
        val file = expandHome(path, home).toPath()
        val result = withContext(dispatcher) {
            try {
                fileSystem.write(file) { writeUtf8(text) }
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
        if (result.isSuccess) written.emit(file)

        return result
    }

    private fun list(directory: Path): List<FileEntry>? {
        val children = fileSystem.listOrNull(directory) ?: return null

        return children
            .filter { it.name != GitDirectoryName }
            .map { FileEntry(name = it.name, path = it.toString(), isDirectory = isDirectory(it)) }
            .sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() }.thenBy { it.name })
    }

    // okio 의 메타데이터는 심볼릭 링크를 따라가지 않는다. 폴더를 가리키는 링크도 폴더로 펼칠 수 있게 푼 경로를 한 번 더 본다.
    private fun isDirectory(path: Path): Boolean {
        val metadata = fileSystem.metadataOrNull(path) ?: return false
        if (metadata.symlinkTarget == null) return metadata.isDirectory

        val resolved = try {
            fileSystem.canonicalize(path)
        } catch (_: IOException) {
            return false
        }

        return fileSystem.metadataOrNull(resolved)?.isDirectory == true
    }

    private fun stamp(file: Path): FileStamp? {
        val metadata = fileSystem.metadataOrNull(file) ?: return null
        if (metadata.isDirectory) return null

        return FileStamp(size = metadata.size, modifiedAt = metadata.lastModifiedAtMillis)
    }

    private fun read(file: Path): FileContent =
        try {
            fileSystem.source(file).buffer().use { source ->
                val buffer = Buffer()
                while (buffer.size < FileContent.FileViewerMaxBytes) {
                    if (source.read(buffer, FileContent.FileViewerMaxBytes - buffer.size) == -1L) break
                }
                val truncated = !source.exhausted()

                fileContentOf(buffer.readByteArray(), truncated)
            }
        } catch (_: IOException) {
            FileContent.Unreadable
        }

    private data class FileStamp(val size: Long?, val modifiedAt: Long?)

    private companion object {
        const val GitDirectoryName = ".git"
    }
}

/** 콜백이 없는 플랫폼의 다시 읽을 신호. 첫 읽기는 `observeOnSignals` 가 하므로 간격만 센다. */
internal fun pollingTicks(interval: Duration): Flow<Unit> =
    flow {
        while (true) {
            delay(interval)
            emit(Unit)
        }
    }

/** 파일 탭에 보일 [bytes] 의 판정. 파일과 git 이 준 커밋 시점 내용이 같이 쓴다. [truncated] 는 [FileContent.FileViewerMaxBytes] 에서 잘랐는지다. */
internal fun fileContentOf(bytes: ByteArray, truncated: Boolean): FileContent =
    if (bytes.take(FileContent.BinarySniffBytes).any { it == 0.toByte() }) {
        FileContent.Binary
    } else {
        FileContent.Text(text = bytes.decodeToString(), truncated = truncated)
    }
