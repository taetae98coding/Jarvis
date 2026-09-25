package io.github.taetae98coding.jarvis.data.profiling

import com.sun.management.OperatingSystemMXBean
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.profiling.DiskActivity
import io.github.taetae98coding.jarvis.domain.profiling.DiskSpace
import io.github.taetae98coding.jarvis.domain.profiling.MemoryUsage
import io.github.taetae98coding.jarvis.domain.profiling.NetworkThroughput
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import io.github.taetae98coding.jarvis.domain.profiling.Usage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

// 데스크탑은 macOS 만 지원한다. 다른 OS 에는 vm_stat·ioreg 가 없어 그 줄은 측정 불가가 된다.
internal actual fun createProfilingSource(context: PlatformContext): ProfilingSource = MacProfilingSource

private object MacProfilingSource : ProfilingSource {
    override val supportedMetrics: Set<ProfilingMetric> = ProfilingMetric.entries.toSet()

    override fun openSampler(): ProfilingSampler = MacProfilingSampler()
}

private class MacProfilingSampler : ProfilingSampler {
    private val os = ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean
    private var cpuPrimed = false
    private val network = CounterDelta()
    private val disk = CounterDelta()

    override suspend fun sample(): Profiling = withContext(Dispatchers.IO) {
        Profiling(
            cpu = readCpu(),
            memory = readMemory(),
            gpu = runCommand("ioreg", "-r", "-d", "1", "-c", "IOAccelerator")
                ?.let(::parseGpuUtilization)
                ?.let(::Usage)
                .toReading(),
            network = network.read(runCommand("netstat", "-ibn")?.let(::parseNetstatBytes)) {
                NetworkThroughput(receivedBytesPerSecond = it.perSecond(0), sentBytesPerSecond = it.perSecond(1))
            },
            diskActivity = disk.read(
                runCommand("ioreg", "-r", "-d", "1", "-c", "IOBlockStorageDriver", "-k", "Statistics")
                    ?.let(::parseDiskBusyNanos),
            ) {
                DiskActivity(readPercent = it.percentOfElapsed(0), writePercent = it.percentOfElapsed(1))
            },
            diskSpace = readDiskSpace(),
        )
    }

    private fun readCpu(): Reading<Usage> {
        val load = os?.cpuLoad ?: return Reading.Unavailable
        // JDK 의 macOS 구현은 직전 호출과의 틱 차이를 낸다. 첫 호출은 비교할 것이 없어 0 을 돌려준다.
        if (!cpuPrimed) {
            cpuPrimed = true
            return Reading.Measuring
        }

        return if (load < 0) Reading.Unavailable else Reading.Available(Usage(percent(load, 1.0)))
    }

    private fun readMemory(): Reading<MemoryUsage> {
        val total = os?.totalMemorySize?.takeIf { it > 0 } ?: return Reading.Unavailable
        val used = runCommand("vm_stat")?.let(::parseVmStatUsedBytes) ?: return Reading.Unavailable

        return Reading.Available(MemoryUsage(usedBytes = used.coerceAtMost(total), totalBytes = total))
    }

    private fun readDiskSpace(): Reading<DiskSpace> =
        runCatching {
            val home = File(System.getProperty("user.home"))
            DiskSpace(freeBytes = home.usableSpace, totalBytes = home.totalSpace).takeIf { it.totalBytes > 0 }
        }.getOrNull().toReading()
}

private fun runCommand(vararg command: String): String? =
    runCatching {
        // 파이프로 받으면 출력이 파이프 버퍼를 넘을 때 waitFor 가 끝나지 않는다. IOAccelerator 는 수십 KB 다.
        val output = File.createTempFile("jarvis-profiling", ".out")

        try {
            val process = ProcessBuilder(*command)
                .redirectOutput(output)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            process.outputStream.close()

            when {
                !process.waitFor(CommandTimeoutSeconds, TimeUnit.SECONDS) -> {
                    process.destroyForcibly()
                    null
                }

                process.exitValue() != 0 -> null
                else -> output.readText()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()

private const val CommandTimeoutSeconds = 5L
