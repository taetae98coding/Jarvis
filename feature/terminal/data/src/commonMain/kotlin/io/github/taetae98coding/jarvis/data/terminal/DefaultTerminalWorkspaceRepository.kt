package io.github.taetae98coding.jarvis.data.terminal

import androidx.datastore.core.DataStore
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceChange
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultTerminalWorkspaceRepository(
    private val store: DataStore<TerminalWorkspaceDto>?,
) : TerminalWorkspaceRepository {
    private val restoreLock = Mutex()
    private var restored = false

    override fun observeWorkspace(): Flow<TerminalWorkspace> {
        if (store == null) return flowOf(TerminalWorkspace.initial())

        return flow {
            restore(store)
            emitAll(store.data.map { it.toDomain() }.distinctUntilChanged())
        }
    }

    override suspend fun updateWorkspace(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange {
        if (store == null) {
            val initial = TerminalWorkspace.initial()
            return TerminalWorkspaceChange(initial, transform(initial))
        }

        restore(store)

        // updateData 는 변경을 한 줄로 세워 한 번씩 부른다. 바꾸기 전 값을 여기서 잡아야 앞의 변경과 섞이지 않는다.
        var before: TerminalWorkspace? = null
        val after = store.updateData { dto ->
            val current = dto.toDomain()
            before = current
            transform(current).toDto()
        }.toDomain()

        return TerminalWorkspaceChange(before ?: after, after)
    }

    /**
     * 앱을 켠 뒤 처음 파일을 만질 때 한 번, 지난 실행이 남긴 실행·명령 탭을 셸 탭으로 돌린다(docs/common/terminal-run.html R18).
     * 저장 형식 → 도메인 변환에서 지우면 켜져 있는 동안 방금 연 탭의 명령도 되읽을 때 사라져 셸만 뜬다.
     */
    private suspend fun restore(store: DataStore<TerminalWorkspaceDto>) {
        restoreLock.withLock {
            if (restored) return
            store.updateData { dto ->
                val current = dto.toDomain()
                val cleared = current.withoutCommands()
                if (cleared === current) dto else cleared.toDto()
            }
            restored = true
        }
    }
}
