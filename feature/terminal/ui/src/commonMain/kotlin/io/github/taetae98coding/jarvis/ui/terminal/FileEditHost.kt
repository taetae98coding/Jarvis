package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.WriteFileUseCase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 마지막 입력 뒤 이만큼 더 고치지 않으면 저장한다(docs/common/terminal-file-editor.html E3). */
internal val FileAutoSaveDelay: Duration = 1.seconds

/**
 * 파일 탭 하나의 편집 상태(docs/common/terminal-file-editor.html E2–E7). [base] 는 마지막으로 디스크와 같다고 본 글이고,
 * [disk] 는 마지막으로 본 디스크 글이다. 둘이 다르고 고친 것이 있으면 디스크가 따로 바뀐 것이다(E6).
 */
internal data class FileEdit(
    val path: String,
    val base: String,
    val text: String,
    val disk: String,
    val saving: Boolean = false,
    val error: String? = null,
) {
    val dirty: Boolean get() = text != base

    val diskChanged: Boolean get() = dirty && disk != base

    /** 자동 저장이 멈춰 사용자가 "저장" 을 눌러야 하는 상태(E4·E6). */
    val needsManualSave: Boolean get() = error != null || diskChanged
}

/**
 * 탭마다 편집 중인 글을 들고 자동 저장한다. 저장하지 않은 글은 같은 그룹의 다른 탭을 보거나 터미널 화면을 떠나도 남아야 하고,
 * 기다리던 저장은 도중에 화면을 떠나도 끝나야 해서 [LineCommentHost] 처럼 앱 수명 스코프를 쓴다(E3, E7).
 */
internal class FileEditHost(
    private val writeFile: WriteFileUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 탭을 닫으며 곧장 쓰는 것과 앞서 시작한 저장이 겹칠 수 있다. 먼저 요청한 글이 나중에 덮어쓰지 않게 한 줄로 세운다.
    private val writes = Mutex()

    private val autoSaves = mutableMapOf<Long, Job>()

    private val _edits = MutableStateFlow<Map<Long, FileEdit>>(emptyMap())
    val edits: StateFlow<Map<Long, FileEdit>> = _edits.asStateFlow()

    fun start(tabId: Long, path: String, text: String) {
        _edits.update { if (tabId in it) it else it + (tabId to FileEdit(path = path, base = text, text = text, disk = text)) }
    }

    fun change(tabId: Long, text: String) {
        edit(tabId) { it.copy(text = text, error = null) }
        scheduleAutoSave(tabId)
    }

    // 고친 것이 없으면 새 디스크 내용을 그대로 받는다. 있으면 글은 두고 바뀌었다는 것만 남긴다(E6).
    fun diskChanged(tabId: Long, text: String) = edit(tabId) { current ->
        when {
            text == current.disk -> current
            current.dirty -> current.copy(disk = text)
            else -> current.copy(base = text, text = text, disk = text)
        }
    }

    /** 내 변경을 버리고 마지막으로 본 디스크 글을 받는다(E6 "디스크 내용으로"). */
    fun revert(tabId: Long) {
        cancelAutoSave(tabId)
        edit(tabId) { it.copy(base = it.disk, text = it.disk, error = null) }
    }

    /** ⌘S·"저장". 기다리지 않고, 디스크가 따로 바뀌었어도 덮어쓴다(E3, E4, E6). */
    fun save(tabId: Long) {
        cancelAutoSave(tabId)
        val current = _edits.value[tabId] ?: return
        if (current.saving || !current.dirty) return

        val text = current.text
        edit(tabId) { it.copy(saving = true, error = null) }
        scope.launch {
            val result = writes.withLock { writeFile(current.path, text) }
            edit(tabId) { edit ->
                result.fold(
                    // 저장한 글을 디스크 글로 본다. 곧 돌아올 자기 저장 결과를 디스크가 바뀐 것으로 치지 않게(E6).
                    onSuccess = { edit.copy(base = text, disk = text, saving = false) },
                    onFailure = { edit.copy(saving = false, error = it.message ?: it.toString()) },
                )
            }
            // 쓰는 동안 더 고친 것은 그때 건 자동 저장이 saving 에 막혔으므로 다시 건다.
            if (_edits.value[tabId]?.dirty == true) scheduleAutoSave(tabId)
        }
    }

    /** 기다리던 자동 저장을 곧장 한다. 읽기 보기·미리보기로 바꿀 때 부른다(E5, M5). */
    fun flush(tabId: Long) {
        val current = _edits.value[tabId] ?: return
        if (!current.needsManualSave) save(tabId)
    }

    /** 닫힌 탭의 편집을 버린다. 기다리던 자동 저장은 곧장 쓰고, 멈춘 변경은 버린다(E7). */
    fun retain(tabIds: Set<Long>) {
        val closed = _edits.value.filterKeys { it !in tabIds }
        if (closed.isEmpty()) return

        closed.forEach { (tabId, edit) ->
            cancelAutoSave(tabId)
            if (edit.dirty && !edit.needsManualSave) scope.launch { writes.withLock { writeFile(edit.path, edit.text) } }
        }
        _edits.update { edits -> edits.filterKeys { it in tabIds } }
    }

    fun close() {
        scope.cancel()
    }

    private fun scheduleAutoSave(tabId: Long) {
        cancelAutoSave(tabId)
        autoSaves[tabId] = scope.launch {
            delay(FileAutoSaveDelay)
            autoSaves.remove(tabId)
            val current = _edits.value[tabId] ?: return@launch
            if (!current.needsManualSave) save(tabId)
        }
    }

    private fun cancelAutoSave(tabId: Long) {
        autoSaves.remove(tabId)?.cancel()
    }

    private fun edit(tabId: Long, transform: (FileEdit) -> FileEdit) {
        _edits.update { edits -> edits[tabId]?.let { edits + (tabId to transform(it)) } ?: edits }
    }
}
