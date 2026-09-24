package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 띄워 둔 셸 하나. [close] 를 부르거나 셸이 끝나면 [output] 이 완료된다. */
interface TerminalSession {
    /**
     * pty 에 붙어 있는지. false 면 에코와 줄 편집을 셸이 하지 않으므로 화면이 [LineDiscipline] 으로
     * 대신한다. 크기 변경도 셸에 닿지 않는다.
     */
    val isPty: Boolean

    /** 한 번만 수집한다. 두 번째 수집자는 첫 수집자와 바이트를 나눠 갖게 된다. */
    val output: Flow<ByteArray>

    /**
     * 셸의 작업 디렉터리. 운영체제가 바뀜을 알려 주지 않아서, 출력이 잠시 멎을 때(명령이 끝나고
     * 프롬프트가 그려질 때)마다 다시 읽는다. 같으면 흘리지 않는다. 읽을 수 없는 세션은 아무것도 흘리지 않는다.
     */
    val directory: Flow<String>
        get() = emptyFlow()

    suspend fun write(bytes: ByteArray)

    fun resize(size: TerminalSize)

    fun close()
}
