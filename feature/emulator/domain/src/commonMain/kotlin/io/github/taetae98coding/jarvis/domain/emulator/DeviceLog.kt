package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DeviceLogLevel {
    Verbose,
    Debug,
    Info,
    Warn,
    Error,
    Fatal,
    Unknown,
}

data class DeviceLogLine(
    val text: String,
    val level: DeviceLogLevel,
)

interface DeviceLogRepository {
    /**
     * [deviceId] 기기의 로그를 새로 온 줄 묶음으로 흘린다. Android 는 logcat, iOS 시뮬레이터는 통합 로그다.
     *
     * 수집하는 동안에만 기기 로그를 읽고, 수집이 끝나면 읽기를 멈춘다. 기기가 사라지면 줄이 오지 않을 뿐 끝나지 않고,
     * 다시 나타나면 이어서 흘린다(docs/common/device-logcat.html R11).
     */
    fun observeLog(deviceId: String): Flow<List<String>>
}

class ObserveDeviceLogUseCase(
    private val repository: DeviceLogRepository,
) {
    operator fun invoke(deviceId: String): Flow<List<DeviceLogLine>> =
        repository.observeLog(deviceId).map { lines -> lines.map { DeviceLogLine(it, parseDeviceLogLevel(it)) } }
}

/** 실물 iOS 기기는 기본 도구로 syslog 를 흘려받을 수 없다(docs/platform/jvm.html#device-logcat). */
val EmulatorDevice.canReadLog: Boolean
    get() = !(platform == EmulatorPlatform.IOS && isPhysical)

/**
 * 줄 모양에서 수준을 읽는다. `logcat -v threadtime` 은 `09-25 17:18:51.832  1850  2652 I tag: …` 로 PID·TID 뒤 한 글자,
 * `log stream --style compact` 는 `2026-09-25 17:18:51.832 Df process[1:2] …` 로 시각 뒤 한두 글자다.
 * 머리글(`--------- beginning of main`, `Filtering the log data …`)은 [DeviceLogLevel.Unknown] 이다.
 */
fun parseDeviceLogLevel(line: String): DeviceLogLevel {
    LogcatLine.find(line)?.let { match ->
        return when (match.groupValues[1]) {
            "V" -> DeviceLogLevel.Verbose
            "D" -> DeviceLogLevel.Debug
            "I" -> DeviceLogLevel.Info
            "W" -> DeviceLogLevel.Warn
            "E" -> DeviceLogLevel.Error
            // A 는 logcat 의 ASSERT(Log.wtf) 다.
            else -> DeviceLogLevel.Fatal
        }
    }

    CompactLine.find(line)?.let { match ->
        return when (match.groupValues[1]) {
            "Db" -> DeviceLogLevel.Debug
            // Df 는 os_log 의 default 수준이다. Info 와 한 단계로 본다.
            "Df", "I", "A" -> DeviceLogLevel.Info
            "E" -> DeviceLogLevel.Error
            else -> DeviceLogLevel.Fatal
        }
    }

    return DeviceLogLevel.Unknown
}

private val LogcatLine = Regex("""^\d\d-\d\d \d\d:\d\d:\d\d\.\d{3}\s+\d+\s+\d+ ([VDIWEFA]) """)

private val CompactLine = Regex("""^\d{4}-\d\d-\d\d \d\d:\d\d:\d\d\.\d{3} (Db|Df|I|E|Fa|F|A) """)
