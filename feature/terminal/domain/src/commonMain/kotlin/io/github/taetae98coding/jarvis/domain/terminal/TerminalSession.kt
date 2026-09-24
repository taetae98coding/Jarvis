package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

/** 띄워 둔 셸 하나. [close] 를 부르거나 셸이 끝나면 [output] 이 완료된다. */
interface TerminalSession {
    /**
     * pty 에 붙어 있는지. false 면 에코와 줄 편집을 셸이 하지 않으므로 화면이 [LineDiscipline] 으로
     * 대신한다. 크기 변경도 셸에 닿지 않는다.
     */
    val isPty: Boolean

    /** 한 번만 수집한다. 두 번째 수집자는 첫 수집자와 바이트를 나눠 갖게 된다. */
    val output: Flow<ByteArray>

    suspend fun write(bytes: ByteArray)

    fun resize(size: TerminalSize)

    fun close()
}
