package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.focus.FocusAlarmRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

internal actual fun createFocusAlarm(context: PlatformContext): FocusAlarmRepository = OsascriptFocusAlarm

internal actual fun localUtcOffsetSeconds(instant: Instant): Int =
    TimeZone.getDefault().getOffset(instant.toEpochMilliseconds()) / MillisPerSecond

/**
 * macOS 알림 센터에 문구를 띄운다. JVM 에는 앱이 꺼진 뒤에도 남는 예약 알림 API 가 없어, 앱 프로세스 안의
 * 타이머가 끝나는 시각에 `osascript` 를 부른다. 앱을 끄면 알림도 없다(docs/platform/jvm.html#focus-timer).
 */
private object OsascriptFocusAlarm : FocusAlarmRepository {
    // 데몬 스레드라 앱 종료를 막지 않는다. 앱 수명 코루틴 스코프를 data 에 두지 않으려고 실행기를 쓴다.
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "jarvis-focus-alarm").apply { isDaemon = true }
    }
    private var pending: ScheduledFuture<*>? = null

    @Synchronized
    override fun schedule(at: Instant, phase: FocusPhase) {
        pending?.cancel(false)
        val delay = (at - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0)
        pending = executor.schedule({ notify(phase) }, delay, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    override fun cancel() {
        pending?.cancel(false)
        pending = null
    }

    private fun notify(phase: FocusPhase) {
        val message = focusAlarmMessage(phase)
        val script = "display notification \"${message.body}\" with title \"Jarvis\" subtitle \"${message.title}\" sound name \"Glass\""

        // 다른 OS 에는 osascript 가 없어 실행이 실패하고 조용히 끝난다.
        runCatching {
            ProcessBuilder("osascript", "-e", script)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }
}

private const val MillisPerSecond = 1000
