package io.github.taetae98coding.jarvis.data.profiling

import android.app.ActivityManager
import android.net.TrafficStats
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.profiling.DiskSpace
import io.github.taetae98coding.jarvis.domain.profiling.MemoryUsage
import io.github.taetae98coding.jarvis.domain.profiling.NetworkThroughput
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingScope
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import io.github.taetae98coding.jarvis.domain.profiling.Usage

internal actual fun createProfilingSource(context: PlatformContext): ProfilingSource =
    AndroidProfilingSource(context.context.getSystemService(ActivityManager::class.java))

// GPU 와 디스크 읽기·쓰기는 일반 앱이 읽을 수 있는 API·파일이 없다(docs/platform/android.html#profiling).
private class AndroidProfilingSource(
    private val activityManager: ActivityManager?,
) : ProfilingSource {
    override val supportedMetrics: Set<ProfilingMetric> =
        setOf(ProfilingMetric.CPU, ProfilingMetric.MEMORY, ProfilingMetric.NETWORK, ProfilingMetric.DISK_SPACE)

    override fun openSampler(): ProfilingSampler {
        val cpu = CounterDelta()
        val network = CounterDelta()

        return ProfilingSampler {
            Profiling(
                cpu = cpu.read(longArrayOf(Process.getElapsedCpuTime(), SystemClock.elapsedRealtime())) {
                    // /proc/stat 이 Android 8 부터 앱에 막혀 이 프로세스의 CPU 시간만 잰다. 코어 수로 나눠 기기 전체 대비 비율로 둔다.
                    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                    Usage(percent(it.deltas[0].toDouble(), it.deltas[1].toDouble() * cores), ProfilingScope.APP)
                },
                memory = readMemory(),
                gpu = Reading.Unavailable,
                network = network.read(readTrafficBytes()) {
                    NetworkThroughput(receivedBytesPerSecond = it.perSecond(0), sentBytesPerSecond = it.perSecond(1))
                },
                diskActivity = Reading.Unavailable,
                diskSpace = readDiskSpace(),
            )
        }
    }

    private fun readMemory(): Reading<MemoryUsage> {
        val manager = activityManager ?: return Reading.Unavailable
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        if (info.totalMem <= 0) return Reading.Unavailable

        return Reading.Available(MemoryUsage(usedBytes = info.totalMem - info.availMem, totalBytes = info.totalMem))
    }

    private fun readTrafficBytes(): LongArray? {
        val received = TrafficStats.getTotalRxBytes()
        val sent = TrafficStats.getTotalTxBytes()
        if (received == TrafficStats.UNSUPPORTED.toLong() || sent == TrafficStats.UNSUPPORTED.toLong()) return null

        return longArrayOf(received, sent)
    }

    private fun readDiskSpace(): Reading<DiskSpace> =
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            DiskSpace(freeBytes = stat.availableBytes, totalBytes = stat.totalBytes).takeIf { it.totalBytes > 0 }
        }.getOrNull().toReading()
}
