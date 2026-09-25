package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

/** 폴더 안의 항목 하나. [path] 는 `~` 를 편 절대 경로다. */
data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
)

/** 파일 탭에 보일 내용. */
sealed interface FileContent {
    /** [truncated] 면 파일이 [FileViewerMaxBytes] 보다 커서 앞부분만 읽었다. */
    data class Text(val text: String, val truncated: Boolean) : FileContent

    /** 앞 [BinarySniffBytes] 안에 NUL 바이트가 있다. */
    data object Binary : FileContent

    /** 없거나, 폴더이거나, 읽을 수 없다. */
    data object Unreadable : FileContent

    companion object {
        const val FileViewerMaxBytes: Long = 512L * 1024

        const val BinarySniffBytes: Int = 8 * 1024
    }
}

interface FileRepository {
    /**
     * [directory] 바로 아래 항목. 폴더 먼저, 그 안에서 이름(대소문자 무시) 순이고 `.git` 은 뺀다. 폴더가 없거나
     * 읽을 수 없거나 파일을 볼 수 없는 플랫폼이면 null 이다. 수집하는 동안만 디스크를 따라간다(cold).
     */
    fun observeDirectory(directory: String): Flow<List<FileEntry>?>

    /** [path] 파일의 내용. 수집하는 동안만 디스크를 따라간다(cold). */
    fun observeFile(path: String): Flow<FileContent>

    /** [path] 파일을 [text] 의 UTF-8 로 덮어쓴다. 끝나면 그 경로를 보고 있는 [observeFile] 이 곧장 다시 읽는다. */
    suspend fun writeFile(path: String, text: String): Result<Unit>
}

class ObserveDirectoryUseCase(
    private val repository: FileRepository,
) {
    operator fun invoke(directory: String): Flow<List<FileEntry>?> = repository.observeDirectory(directory)
}

class ObserveFileUseCase(
    private val repository: FileRepository,
) {
    operator fun invoke(path: String): Flow<FileContent> = repository.observeFile(path)
}

class WriteFileUseCase(
    private val repository: FileRepository,
) {
    suspend operator fun invoke(path: String, text: String): Result<Unit> = repository.writeFile(path, text)
}
