package io.github.taetae98coding.jarvis.data.terminal

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

/**
 * 작업 공간을 저장할 DataStore. 한 파일에 DataStore 는 하나만 있어야 해서 Koin 이 한 번만 부른다.
 * 터미널을 쓸 수 없는 타깃은 null 이고, 그러면 저장하지 않는다.
 */
internal expect fun createTerminalWorkspaceStore(context: PlatformContext): DataStore<TerminalWorkspaceDto>?

internal const val TerminalWorkspaceFileName = "terminal-workspace.json"

internal fun terminalWorkspaceStore(fileSystem: FileSystem, path: Path): DataStore<TerminalWorkspaceDto> =
    DataStoreFactory.create(
        storage = OkioStorage(fileSystem, TerminalWorkspaceSerializer, producePath = { path }),
        corruptionHandler = ReplaceFileCorruptionHandler { TerminalWorkspaceSerializer.defaultValue },
    )

internal object TerminalWorkspaceSerializer : OkioSerializer<TerminalWorkspaceDto> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: TerminalWorkspaceDto
        get() = TerminalWorkspace.initial().toDto()

    override suspend fun readFrom(source: BufferedSource): TerminalWorkspaceDto =
        try {
            json.decodeFromString(TerminalWorkspaceDto.serializer(), source.readUtf8())
        } catch (e: SerializationException) {
            throw CorruptionException("terminal workspace is not valid JSON", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("terminal workspace has an unexpected shape", e)
        }

    override suspend fun writeTo(t: TerminalWorkspaceDto, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(TerminalWorkspaceDto.serializer(), t))
    }
}
