package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.LineDiscipline
import io.github.taetae98coding.jarvis.domain.terminal.TerminalEmulator
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.encodeTerminalWheel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 창 하나의 셸과 에뮬레이터. [scope] 는 메인 스레드여야 한다 — 에뮬레이터는 메인에서만 만진다.
 *
 * 셸이 스스로 끝나면 [onExit] 이 불린다. [close] 로 닫을 때는 불리지 않는다. 셸의 작업 디렉터리가 바뀌면
 * [onDirectory] 가 불린다.
 */
internal class TerminalPaneState(
    val id: Long,
    initialSize: TerminalSize,
    private val scope: CoroutineScope,
    open: suspend (TerminalSize) -> TerminalSession?,
    private val onExit: (Long) -> Unit,
    private val onDirectory: (Long, String) -> Unit = { _, _ -> },
) {
    val emulator = TerminalEmulator(initialSize.columns, initialSize.rows)

    // 에뮬레이터는 Compose 상태가 아니다. 바뀔 때마다 이 값을 올려 그리기를 다시 부른다.
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /** 화면 맨 아래에서 거슬러 올라간 줄 수. 0 이면 최신 화면이다. */
    private val _scrollOffset = MutableStateFlow(0)
    val scrollOffset: StateFlow<Int> = _scrollOffset.asStateFlow()
    private var scrollRemainder = 0f

    /** OSC 0/2 로 셸이 정한 제목. 탭 이름이 된다. */
    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    private var session: TerminalSession? = null
    private var discipline: LineDiscipline? = null

    // 쓰기마다 코루틴을 띄우면 IO 스레드에서 순서가 뒤바뀐다. 한 줄로 세운다.
    private val writes = Channel<ByteArray>(Channel.UNLIMITED)

    private val job = scope.launch {
        // 셸을 띄우는 도중에 닫히면 만들어진 프로세스를 돌려받지 못해 새어 나간다. 끝까지 기다린 뒤
        // 닫혔으면 직접 치운다.
        val opened = withContext(NonCancellable) { open(TerminalSize(emulator.columns, emulator.rows)) }

        if (opened == null) {
            emulator.feed("[셸을 시작할 수 없습니다]")
            invalidate()
            return@launch
        }

        if (!currentCoroutineContext().isActive) {
            opened.close()
            return@launch
        }

        session = opened
        discipline = if (opened.isPty) null else LineDiscipline()
        // 셸이 뜨는 동안 창이 배치되어 크기가 바뀌었을 수 있다.
        opened.resize(TerminalSize(emulator.columns, emulator.rows))

        launch {
            for (bytes in writes) opened.write(bytes)
        }
        launch {
            opened.directory.collect { onDirectory(id, it) }
        }

        opened.output.collect { bytes ->
            emulator.feed(discipline?.output(bytes) ?: bytes)

            val responses = emulator.drainResponses()
            if (responses.isNotEmpty()) writes.trySend(responses)

            _title.value = emulator.title
            // ESC[3J·대체 화면 진입으로 스크롤백이 줄면 보던 자리가 그 밖에 남는다.
            _scrollOffset.update { it.coerceAtMost(emulator.scrollbackSize) }
            invalidate()
        }

        onExit(id)
    }

    fun input(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        scrollToBottom()

        val discipline = discipline
        if (discipline == null) {
            writes.trySend(bytes)
            return
        }

        val result = discipline.input(bytes)
        if (result.echo.isNotEmpty()) {
            emulator.feed(result.echo)
            invalidate()
        }
        if (result.send.isNotEmpty()) writes.trySend(result.send)
    }

    /** 붙여넣기. 여러 글자면 셸·프로그램이 bracketed paste 를 켰을 때 그 표시로 감싼다. */
    fun paste(text: String) {
        input(pastedBytes(text, emulator.bracketedPaste))
    }

    /**
     * 창이 지금 Claude Code 의 입력을 보이고 있는지. 셸의 줄 편집기도 bracketed paste 는 켜므로 포커스 보고까지 본다
     * (docs/common/terminal-line-comment.html#claude-ready).
     */
    val readyForClaudeInput: Boolean
        get() = emulator.bracketedPaste && emulator.focusReporting

    fun resize(columns: Int, rows: Int) {
        if (columns == emulator.columns && rows == emulator.rows) return

        emulator.resize(columns, rows)
        session?.resize(TerminalSize(emulator.columns, emulator.rows))
        _scrollOffset.update { it.coerceAtMost(emulator.scrollbackSize) }
        invalidate()
    }

    /**
     * 휠·드래그. 양수면 과거로 올라간다. 줄 높이보다 작은 움직임은 모아 둔다. [column]·[row] 는 휠이 있는 칸이다.
     * 프로그램이 마우스 보고나 대체 화면을 켰으면 스크롤백 대신 그 프로그램에 보낸다(docs/common/terminal-scroll.html).
     */
    fun scrollBy(pixels: Float, lineHeight: Float, column: Int, row: Int) {
        if (lineHeight <= 0f) return

        scrollRemainder += pixels
        val lines = (scrollRemainder / lineHeight).toInt()
        if (lines == 0) return

        scrollRemainder -= lines * lineHeight

        val bytes = encodeTerminalWheel(emulator, lines, column, row)
        if (bytes != null) {
            input(bytes)
            return
        }

        _scrollOffset.update { (it + lines).coerceIn(0, emulator.scrollbackSize) }
    }

    fun close() {
        job.cancel()
        writes.close()
        session?.close()
    }

    private fun scrollToBottom() {
        scrollRemainder = 0f
        _scrollOffset.value = 0
    }

    private fun invalidate() {
        _revision.update { it + 1 }
    }
}

internal fun pastedBytes(text: String, bracketedPaste: Boolean): ByteArray {
    // 소프트 키보드의 Enter 는 키 이벤트가 아니라 줄바꿈 글자로 온다. 붙여넣기도 터미널처럼 줄바꿈을 CR 로 보낸다.
    val normalized = text.replace("\r\n", "\r").replace('\n', '\r')
    val wrapped = if (normalized.length > 1 && bracketedPaste) "\u001b[200~$normalized\u001b[201~" else normalized

    return wrapped.encodeToByteArray()
}
