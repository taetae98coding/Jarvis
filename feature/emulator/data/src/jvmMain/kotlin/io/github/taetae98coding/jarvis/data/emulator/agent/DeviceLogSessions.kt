package io.github.taetae98coding.jarvis.data.emulator.agent

import io.github.taetae98coding.jarvis.data.emulator.DeviceLogDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 에이전트가 기기마다 로그를 한 벌 읽어 최근 [capacity] 줄을 순번과 함께 들고 있는다. 다른 타깃은 순번 커서로 뒤의 줄만 묻는다.
 * [idleTimeout] 동안 묻는 곳이 없는 기기는 [sweep] 이 읽기를 끊는다 — 클라이언트는 끊는다고 알려 주지 않는다.
 *
 * HTTP 처리 스레드와 수집 코루틴이 함께 만지므로 [lock] 으로 묶는다.
 */
internal class DeviceLogSessions(
    private val dataSource: DeviceLogDataSource,
    private val scope: CoroutineScope,
    private val capacity: Int = AgentLogCapacity,
    private val idleTimeout: Duration = AgentLogIdleTimeout,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Session {
        val lines = ArrayDeque<Pair<Long, String>>()
        var lastRead = 0L
        var job: Job? = null
    }

    private val lock = Any()
    private val sessions = HashMap<String, Session>()
    private var sequence = 0L

    fun read(deviceId: String, after: Long): DeviceLogChunk =
        synchronized(lock) {
            val session = sessions.getOrPut(deviceId) { start(deviceId) }
            session.lastRead = clock()

            val lines = session.lines.filter { it.first > after }.map { it.second }
            val next = maxOf(after, session.lines.lastOrNull()?.first ?: after)

            DeviceLogChunk(next = next, lines = lines)
        }

    fun sweep() {
        synchronized(lock) {
            val now = clock()
            val idle = sessions.filterValues { now - it.lastRead > idleTimeout.inWholeMilliseconds }

            idle.forEach { (deviceId, session) ->
                session.job?.cancel()
                sessions.remove(deviceId)
            }
        }
    }

    private fun start(deviceId: String): Session {
        val session = Session()

        session.job = scope.launch {
            dataSource.observeLog(deviceId).collect { batch ->
                synchronized(lock) {
                    batch.forEach { line -> session.lines.addLast(++sequence to line) }
                    while (session.lines.size > capacity) session.lines.removeFirst()
                }
            }
        }

        return session
    }
}

private const val AgentLogCapacity = 2000

// 클라이언트는 1초마다 묻는다. 탭이 잠깐 가려졌다 돌아오는 정도로는 줄을 버리지 않을 만큼 둔다.
private val AgentLogIdleTimeout = 10.seconds

internal val AgentLogSweepInterval = 1.seconds
