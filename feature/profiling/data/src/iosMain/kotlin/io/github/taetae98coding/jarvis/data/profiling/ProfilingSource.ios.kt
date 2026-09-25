package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.profiling.DiskSpace
import io.github.taetae98coding.jarvis.domain.profiling.MemoryUsage
import io.github.taetae98coding.jarvis.domain.profiling.NetworkThroughput
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import io.github.taetae98coding.jarvis.domain.profiling.Usage
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSURLVolumeAvailableCapacityForImportantUsageKey
import platform.Foundation.NSURLVolumeTotalCapacityKey
import platform.darwin.CPU_STATE_IDLE
import platform.darwin.CPU_STATE_MAX
import platform.darwin.CPU_STATE_NICE
import platform.darwin.CPU_STATE_SYSTEM
import platform.darwin.CPU_STATE_USER
import platform.darwin.HOST_CPU_LOAD_INFO
import platform.darwin.HOST_VM_INFO64
import platform.darwin.KERN_SUCCESS
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.host_cpu_load_info
import platform.darwin.host_statistics
import platform.darwin.host_statistics64
import platform.darwin.ifaddrs
import platform.darwin.integer_tVar
import platform.darwin.mach_host_self
import platform.darwin.mach_msg_type_number_tVar
import platform.darwin.vm_statistics64
import platform.posix.AF_LINK
import platform.posix.getpagesize
import platform.posix.if_data

internal actual fun createProfilingSource(context: PlatformContext): ProfilingSource = DarwinProfilingSource

// GPU 와 디스크 읽기·쓰기는 IOKit 이 샌드박스 밖이라 줄 수 없다(docs/platform/ios.html#profiling).
private object DarwinProfilingSource : ProfilingSource {
    override val supportedMetrics: Set<ProfilingMetric> =
        setOf(ProfilingMetric.CPU, ProfilingMetric.MEMORY, ProfilingMetric.NETWORK, ProfilingMetric.DISK_SPACE)

    override fun openSampler(): ProfilingSampler {
        val cpu = CounterDelta()
        val network = CounterDelta()

        return ProfilingSampler {
            Profiling(
                cpu = cpu.read(readCpuTicks()) {
                    Usage(it.percentOf(part = BusyTicks, whole = AllTicks))
                },
                memory = readMemory(),
                gpu = Reading.Unavailable,
                network = network.read(readInterfaceBytes()) {
                    NetworkThroughput(receivedBytesPerSecond = it.perSecond(0), sentBytesPerSecond = it.perSecond(1))
                },
                diskActivity = Reading.Unavailable,
                diskSpace = readDiskSpace(),
            )
        }
    }
}

// readCpuTicks 는 cpu_ticks 를 CPU_STATE_* 순서 그대로 돌려준다.
private val BusyTicks = intArrayOf(CPU_STATE_USER, CPU_STATE_SYSTEM, CPU_STATE_NICE)
private val AllTicks = BusyTicks + CPU_STATE_IDLE

@OptIn(ExperimentalForeignApi::class)
private fun readCpuTicks(): LongArray? = memScoped {
    val info = alloc<host_cpu_load_info>()
    // HOST_CPU_LOAD_INFO_COUNT 는 sizeof 를 쓰는 매크로라 cinterop 이 내보내지 않는다.
    val count = alloc<mach_msg_type_number_tVar>()
    count.value = (sizeOf<host_cpu_load_info>() / sizeOf<integer_tVar>()).toUInt()

    if (host_statistics(mach_host_self(), HOST_CPU_LOAD_INFO, info.ptr.reinterpret(), count.ptr) != KERN_SUCCESS) {
        return null
    }

    LongArray(CPU_STATE_MAX) { info.cpu_ticks[it].toLong() }
}

/** macOS 활동 모니터의 "사용된 메모리" 와 같은 식이다. */
@OptIn(ExperimentalForeignApi::class)
private fun readMemory(): Reading<MemoryUsage> = memScoped {
    val stats = alloc<vm_statistics64>()
    val count = alloc<mach_msg_type_number_tVar>()
    count.value = (sizeOf<vm_statistics64>() / sizeOf<integer_tVar>()).toUInt()

    if (host_statistics64(mach_host_self(), HOST_VM_INFO64, stats.ptr.reinterpret(), count.ptr) != KERN_SUCCESS) {
        return Reading.Unavailable
    }

    val pages = (stats.internal_page_count.toLong() - stats.purgeable_count.toLong()).coerceAtLeast(0) +
        stats.wire_count.toLong() + stats.compressor_page_count.toLong()
    val total = NSProcessInfo.processInfo.physicalMemory.toLong()
    if (total <= 0) return Reading.Unavailable

    Reading.Available(MemoryUsage(usedBytes = (pages * getpagesize()).coerceAtMost(total), totalBytes = total))
}

/** 루프백을 뺀 인터페이스의 누적 `[받은 바이트, 보낸 바이트]`. if_data 의 카운터는 32비트다. */
@OptIn(ExperimentalForeignApi::class)
private fun readInterfaceBytes(): LongArray? = memScoped {
    val list = alloc<CPointerVar<ifaddrs>>()
    if (getifaddrs(list.ptr) != 0) return null

    try {
        var received = 0L
        var sent = 0L
        var cursor = list.value

        while (cursor != null) {
            val entry = cursor.pointed
            val name = entry.ifa_name?.toKString().orEmpty()
            val data = entry.ifa_data?.reinterpret<if_data>()?.pointed

            if (entry.ifa_addr?.pointed?.sa_family?.toInt() == AF_LINK && data != null && !name.startsWith("lo")) {
                received += data.ifi_ibytes.toLong()
                sent += data.ifi_obytes.toLong()
            }

            cursor = entry.ifa_next
        }

        longArrayOf(received, sent)
    } finally {
        freeifaddrs(list.value)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readDiskSpace(): Reading<DiskSpace> {
    val values = NSURL.fileURLWithPath(NSHomeDirectory())
        .resourceValuesForKeys(listOf(NSURLVolumeAvailableCapacityForImportantUsageKey, NSURLVolumeTotalCapacityKey), null)
        ?: return Reading.Unavailable
    val free = (values[NSURLVolumeAvailableCapacityForImportantUsageKey] as? NSNumber)?.longLongValue
    val total = (values[NSURLVolumeTotalCapacityKey] as? NSNumber)?.longLongValue
    if (free == null || total == null || total <= 0) return Reading.Unavailable

    return Reading.Available(DiskSpace(freeBytes = free, totalBytes = total))
}
