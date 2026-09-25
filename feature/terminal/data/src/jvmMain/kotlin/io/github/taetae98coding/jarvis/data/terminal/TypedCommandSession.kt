package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 셸이 입력을 받을 준비가 되면 [command] 와 Enter 를 한 번 써 넣어, 사용자가 프롬프트에 친 것처럼 돌린다
 * (docs/common/terminal-run.html R16).
 *
 * 준비되기 전에 쓰면 셸이 아직 canonical 모드라 커널 tty 가 글자를 먼저 메아리쳐서, 프롬프트 앞에 명령이 한 번 더 찍힌다.
 * 준비 신호는 zsh·bash 5.1+·fish 가 줄 편집기를 켤 때 내는 괄호 붙여넣기 켜기([BracketedPasteOn])다. 신호를 본 뒤에는
 * 명령을 붙여넣기 괄호로 감싸 탭·`#` 같은 글자가 자동 완성·단축키로 해석되지 않게 한다. 신호를 끈 셸(macOS 기본
 * bash 3.2 등)을 위해 [readyTimeout] 이 지나면 감싸지 않고 그냥 쓴다. 후보와 버린 이유는 docs/platform/jvm.html#terminal-run.
 */
internal class TypedCommandSession(
    private val delegate: TerminalSession,
    private val command: String,
    private val readyTimeout: Duration = TypedCommandReadyTimeout,
) : TerminalSession by delegate {
    private val typeLock = Mutex()
    private var typed = false

    // 수집이 시작될 때 타이머가 돌고, 출력이 끝나거나 수집이 취소되면 타이머도 끝난다.
    override val output: Flow<ByteArray> = channelFlow {
        val timeout = launch {
            delay(readyTimeout)
            type(bracketed = false)
        }
        val ready = SequenceMatcher(BracketedPasteOn)

        delegate.output.collect { bytes ->
            send(bytes)
            if (!typed && ready.feed(bytes)) {
                timeout.cancel()
                type(bracketed = true)
            }
        }
        timeout.cancel()
    }

    private suspend fun type(bracketed: Boolean) {
        typeLock.withLock {
            if (typed) return
            typed = true
        }
        val text = if (bracketed) "$BracketedPasteStart$command$BracketedPasteEnd" else command
        delegate.write((text + Enter).encodeToByteArray())
    }
}

/** 청크 경계에 걸쳐 나뉜 [pattern] 도 찾는다. 한 번 찾으면 계속 참이다. */
internal class SequenceMatcher(private val pattern: ByteArray) {
    private var matched = 0

    val found: Boolean
        get() = matched == pattern.size

    fun feed(bytes: ByteArray): Boolean {
        if (found) return true
        for (byte in bytes) {
            // 앞부분이 어긋나면 처음부터 다시 맞춘다. 패턴에 되풀이되는 접두사가 없어서(ESC 가 한 번뿐) 이것으로 충분하다.
            matched = if (byte == pattern[matched]) matched + 1 else if (byte == pattern[0]) 1 else 0
            if (found) return true
        }
        return false
    }
}

internal val TypedCommandReadyTimeout: Duration = 3.seconds

private const val Enter = "\r"
private const val BracketedPasteStart = "\u001b[200~"
private const val BracketedPasteEnd = "\u001b[201~"
internal val BracketedPasteOn: ByteArray = "\u001b[?2004h".encodeToByteArray()
