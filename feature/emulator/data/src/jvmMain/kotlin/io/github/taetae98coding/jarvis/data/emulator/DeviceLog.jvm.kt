package io.github.taetae98coding.jarvis.data.emulator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal actual val deviceLogDataSource: DeviceLogDataSource = object : DeviceLogDataSource {
    // 꺼진 AVD 는 화면이 부른 쪽에서 켜진 시리얼로 풀어 온다. 실물 iOS 는 읽을 도구가 없다.
    override fun observeLog(deviceId: String): Flow<List<String>> =
        when {
            isAdbSerial(deviceId) -> androidSdkDirectory()?.let { sdk -> observeLogcat(sdk, deviceId) } ?: emptyFlow()
            SimulatorUdid.matches(deviceId) -> observeSimulatorLog(deviceId)
            else -> emptyFlow()
        }
}

/**
 * 처음에는 최근 [LogcatBacklog] 줄부터, 기기가 사라져 logcat 이 끝난 뒤 다시 띄울 때는 마지막으로 받은 줄의 시각부터 읽는다.
 * 그래야 다시 붙을 때 같은 1000줄이 겹쳐 오지 않는다(docs/common/device-logcat.html R11).
 */
private fun observeLogcat(sdk: File, serial: String): Flow<List<String>> =
    flow {
        val adb = adbBinary(sdk)
        // 데몬이 떠 있지 않으면 logcat 을 띄운 adb 가 데몬을 fork 하고, 데몬이 stdout 파이프를 물려받아 logcat 을 죽여도
        // 파이프가 닫히지 않는다. 출력을 파일로 받는 runCommand 로 데몬을 먼저 띄운다.
        runCommand(listOf(adb, "start-server"))

        var since: String? = null

        while (true) {
            processLines(logcatCommand(adb, serial, since)).collect { lines ->
                emit(lines)
                lines.asReversed().firstNotNullOfOrNull(::logcatTimestamp)?.let { since = it }
            }
            delay(LogRestartDelay)
        }
    }.flowOn(Dispatchers.IO)

// `log stream` 에는 지난 줄을 붙이는 옵션이 없어서 다시 띄워도 겹치지 않는다.
private fun observeSimulatorLog(udid: String): Flow<List<String>> =
    flow {
        val simctl = xcodeToolCommand("simctl") ?: return@flow

        while (true) {
            processLines(simulatorLogCommand(simctl, udid)).collect { emit(it) }
            delay(LogRestartDelay)
        }
    }.flowOn(Dispatchers.IO)

internal fun logcatCommand(adb: String, serial: String, since: String?): List<String> =
    listOf(adb, "-s", serial, "logcat", "-v", "threadtime", "-T", since ?: LogcatBacklog.toString())

internal fun simulatorLogCommand(simctl: List<String>, udid: String): List<String> =
    simctl + listOf("spawn", udid, "log", "stream", "--style", "compact")

/** threadtime 줄 맨 앞의 `MM-DD hh:mm:ss.mmm`. `logcat -T` 가 그 모양 그대로 받는다. */
internal fun logcatTimestamp(line: String): String? = LogcatTimestamp.find(line)?.value

/**
 * [command] 의 stdout 을 줄 묶음으로 흘린다. 읽는 동안 쌓인 줄을 한 번에 꺼내 묶어서, 줄마다 화면이 다시 그려지지 않게 한다.
 * 수집이 끝나면 프로세스를 죽인다. 프로세스가 스스로 끝나면 흐름도 끝난다.
 */
private fun processLines(command: List<String>): Flow<List<String>> =
    channelFlow {
        val process = runCatching {
            ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        }.getOrNull() ?: return@channelFlow
        process.outputStream.close()

        val lines = Channel<String>(LineBufferCapacity)

        // 블로킹 readLine 은 취소로 풀리지 않는다. 아래 finally 가 프로세스를 죽여 파이프를 닫아야 끝난다.
        launch(Dispatchers.IO) {
            runCatching {
                process.inputStream.bufferedReader().useLines { sequence -> sequence.forEach { lines.send(it) } }
            }
            lines.close()
        }

        try {
            while (true) {
                val first = lines.receiveCatching().getOrNull() ?: break
                val batch = mutableListOf(first)
                while (batch.size < MaxBatchLines) batch += lines.tryReceive().getOrNull() ?: break
                send(batch)
            }
        } finally {
            process.destroy()
        }
    }

private const val LogcatBacklog = 1000

private const val LineBufferCapacity = 4096

private const val MaxBatchLines = 1000

// 기기가 없으면 logcat 은 곧바로 끝난다. 다시 꽂히기를 기다리며 너무 자주 띄우지 않을 만큼.
private val LogRestartDelay = 2.seconds

private val LogcatTimestamp = Regex("""^\d\d-\d\d \d\d:\d\d:\d\d\.\d{3}""")
