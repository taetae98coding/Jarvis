package io.github.taetae98coding.jarvis.data.terminal

import androidx.datastore.core.DataStore
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceChange
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class DefaultTerminalWorkspaceRepository(
    private val store: DataStore<TerminalWorkspaceDto>?,
) : TerminalWorkspaceRepository {
    override fun observeWorkspace(): Flow<TerminalWorkspace> =
        store?.data?.map { it.toDomain() }?.distinctUntilChanged() ?: flowOf(TerminalWorkspace.initial())

    override suspend fun updateWorkspace(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange {
        if (store == null) {
            val initial = TerminalWorkspace.initial()
            return TerminalWorkspaceChange(initial, transform(initial))
        }

        // updateData 는 변경을 한 줄로 세워 한 번씩 부른다. 바꾸기 전 값을 여기서 잡아야 앞의 변경과 섞이지 않는다.
        var before: TerminalWorkspace? = null
        val after = store.updateData { dto ->
            val current = dto.toDomain()
            before = current
            transform(current).toDto()
        }.toDomain()

        return TerminalWorkspaceChange(before ?: after, after)
    }
}
