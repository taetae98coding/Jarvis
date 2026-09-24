package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration.Companion.milliseconds

/**
 * 셸의 작업 디렉터리를 셸 출력이 멎을 때마다 [read] 로 다시 읽는다.
 *
 * 작업 디렉터리가 바뀌었다는 콜백은 운영체제에 없다. 디렉터리는 명령을 친 뒤에만 바뀌고 명령이 끝나면
 * 셸이 프롬프트를 그리므로, 출력이 멎는 순간을 신호로 쓴다. 출력이 없는 동안에는 읽지 않는다.
 * 버린 후보(OSC 7, 폴링)는 docs/common/terminal-panels.html#implementation 에 있다.
 */
internal class DirectoryTracker(private val read: () -> String?) {
    private val activity = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** 읽기 루프가 덩어리를 받을 때마다 부른다. */
    fun onOutput() {
        activity.tryEmit(Unit)
    }

    @OptIn(FlowPreview::class)
    val directory: Flow<String> =
        activity
            .onStart { emit(Unit) }
            .debounce(DirectoryReadDelay)
            .mapNotNull { runCatching(read).getOrNull() }
            .distinctUntilChanged()
}

internal val DirectoryReadDelay = 500.milliseconds
