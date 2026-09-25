package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.FileRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultFileRepository(
    private val dataSource: FileDataSource,
) : FileRepository {
    override fun observeDirectory(directory: String): Flow<List<FileEntry>?> = dataSource.observeDirectory(directory)

    override fun observeFile(path: String): Flow<FileContent> = dataSource.observeFile(path)

    override suspend fun writeFile(path: String, text: String): Result<Unit> = dataSource.writeFile(path, text)
}
