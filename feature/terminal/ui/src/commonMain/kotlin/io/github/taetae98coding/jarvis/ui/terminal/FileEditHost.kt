package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.WriteFileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
}

/**
 * 탭마다 편집 중인 글을 들고 저장한다. 저장하지 않은 글은 같은 그룹의 다른 탭을 보거나 터미널 화면을 떠나도 남아야 하고,
 * 저장은 도중에 화면을 떠나도 끝나야 해서 [LineCommentHost] 처럼 앱 수명 스코프를 쓴다(E7).
 */
internal class FileEditHost(
    private val writeFile: WriteFileUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _edits = MutableStateFlow<Map<Long, FileEdit>>(emptyMap())
    val edits: StateFlow<Map<Long, FileEdit>> = _edits.asStateFlow()

    fun start(tabId: Long, path: String, text: String) {
        _edits.update { if (tabId in it) it else it + (tabId to FileEdit(path = path, base = text, text = text, disk = text)) }
    }

    fun change(tabId: Long, text: String) = edit(tabId) { it.copy(text = text, error = null) }

    // 고친 것이 없으면 새 디스크 내용을 그대로 받는다. 있으면 글은 두고 바뀌었다는 것만 남긴다(E6).
    fun diskChanged(tabId: Long, text: String) = edit(tabId) { current ->
        when {
            text == current.disk -> current
            current.dirty -> current.copy(disk = text)
            else -> current.copy(base = text, text = text, disk = text)
        }
    }

    fun save(tabId: Long) {
        val current = _edits.value[tabId] ?: return
        if (current.saving || !current.dirty) return

        val text = current.text
        edit(tabId) { it.copy(saving = true, error = null) }
        scope.launch {
            val result = writeFile(current.path, text)
            edit(tabId) { edit ->
                result.fold(
                    // 저장한 글을 디스크 글로 본다. 곧 돌아올 자기 저장 결과를 디스크가 바뀐 것으로 치지 않게(E6).
                    onSuccess = { edit.copy(base = text, disk = text, saving = false) },
                    onFailure = { edit.copy(saving = false, error = it.message ?: it.toString()) },
                )
            }
        }
    }

    fun discard(tabId: Long) {
        _edits.update { it - tabId }
    }

    /** 닫힌 탭의 편집을 버린다. */
    fun retain(tabIds: Set<Long>) {
        _edits.update { edits -> edits.filterKeys { it in tabIds } }
    }

    fun close() {
        scope.cancel()
    }

    private fun edit(tabId: Long, transform: (FileEdit) -> FileEdit) {
        _edits.update { edits -> edits[tabId]?.let { edits + (tabId to transform(it)) } ?: edits }
    }
}
