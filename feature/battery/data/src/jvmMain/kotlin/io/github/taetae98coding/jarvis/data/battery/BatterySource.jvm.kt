package io.github.taetae98coding.jarvis.data.battery

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.data.state.observeByPolling
import io.github.taetae98coding.jarvis.domain.battery.BatteryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

// 데스크톱은 macOS 만 지원한다. 다른 OS 에는 pmset 이 없어 실행이 실패하고 측정 불가가 된다.
internal actual fun createBatterySource(context: PlatformContext): BatterySource = MacBatterySource

private object MacBatterySource : BatterySource {
    override fun observe(): Flow<BatteryStatus> = observeByPolling(BatteryPollInterval, ::read)

    private suspend fun read(): BatteryStatus = withContext(Dispatchers.IO) {
        val battery = runCommand("pmset", "-g", "batt") ?: return@withContext BatteryStatus.Unavailable
        parsePmsetBattery(battery, lowPowerMode = runCommand("pmset", "-g")?.let(::parsePmsetLowPowerMode))
    }
}

// 변경 알림(IOPSNotificationCreateRunLoopSource)은 JNI·JNA 없이 부를 수 없어 폴링한다(docs/platform/jvm.html#battery).
// 잔량은 분 단위로 1% 씩 움직이지만 전원을 꽂고 뽑은 것은 곧 보여야 해서 5초로 둔다.
internal val BatteryPollInterval = 5.seconds

private fun runCommand(vararg command: String): String? =
    runCatching {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        process.outputStream.close()
        // 출력이 1KB 안팎이라 파이프 버퍼를 넘지 않는다. 먼저 다 읽고 기다린다.
        val output = process.inputStream.bufferedReader().readText()

        when {
            !process.waitFor(CommandTimeoutSeconds, TimeUnit.SECONDS) -> {
                process.destroyForcibly()
                null
            }

            process.exitValue() != 0 -> null
            else -> output
        }
    }.getOrNull()

private const val CommandTimeoutSeconds = 5L
