package io.github.taetae98coding.jarvis.data.profiling

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 출력은 개발 머신(macOS 26, Apple Silicon)에서 받은 것을 줄였다.
class MacProfilingParsersTest {
    @Test
    fun vmStatUsedMemoryMatchesActivityMonitorFormula() {
        val output = """
            Mach Virtual Memory Statistics: (page size of 16384 bytes)
            Pages free:                                     6487.
            Pages active:                                 669786.
            Pages wired down:                             247056.
            Pages purgeable:                                3819.
            "Translation faults":                     8019997606.
            Anonymous pages:                              975967.
            Pages occupied by compressor:                 468529.
        """.trimIndent()

        assertEquals((975_967L - 3_819 + 247_056 + 468_529) * 16_384, parseVmStatUsedBytes(output))
    }

    @Test
    fun vmStatWithoutExpectedKeysIsUnreadable() {
        assertNull(parseVmStatUsedBytes("Mach Virtual Memory Statistics: (page size of 4096 bytes)\nPages free: 1."))
        assertNull(parseVmStatUsedBytes("vm_stat: command not found"))
    }

    @Test
    fun gpuUtilizationTakesTheBusiestAccelerator() {
        val output = """
            | "PerformanceStatistics" = {"Tiler Utilization %"=15,"Renderer Utilization %"=14,"Device Utilization %"=15}
            | "PerformanceStatistics" = {"Device Utilization %"=42}
        """.trimMargin()

        assertEquals(42.0, parseGpuUtilization(output))
        assertNull(parseGpuUtilization(""))
    }

    @Test
    fun netstatSumsLinkRowsExceptLoopback() {
        val output = """
            Name       Mtu   Network       Address            Ipkts Ierrs     Ibytes    Opkts Oerrs     Obytes  Coll
            lo0        16384 <Link#1>                      29896357     0 20659348575 29896357     0 20659348575     0
            lo0        16384 127           127.0.0.1       29896357     - 20659348575 29896357     - 20659348575     -
            gif0*      1280  <Link#2>                             0     0          0        0     0          0     0
            en1        1500  <Link#23>   4e:80:45:30:a2:e6 77098280     0 83145180657 30401936     0 12511735661     0
            en1        1500  192.168.0     192.168.0.10    77098280     - 83145180657 30401936     - 12511735661     -
            awdl0      1500  <Link#25>   8e:a4:b6:40:70:e0    71131     0   18341349    71309     0   18846656     0
            utun3      1380  <Link#30>                          100     0       5000      200     0       7000     0
        """.trimIndent()

        assertContentEquals(
            longArrayOf(83_145_180_657 + 18_341_349 + 5_000, 12_511_735_661 + 18_846_656 + 7_000),
            parseNetstatBytes(output),
        )
    }

    @Test
    fun netstatWithoutHeaderIsUnreadable() {
        assertNull(parseNetstatBytes("netstat: sysctl: Operation not permitted"))
    }

    @Test
    fun diskBusyTimeSumsEveryDriver() {
        val output = """
            |   "Statistics" = {"Operations (Write)"=0,"Total Time (Read)"=0,"Total Time (Write)"=0,"Bytes (Write)"=0}
            |   "Statistics" = {"Total Time (Read)"=133749546712057,"Latency Time (Read)"=0,"Total Time (Write)"=5756963243308}
            |   "Statistics" = {"Total Time (Read)"=152441447,"Total Time (Write)"=10}
        """.trimMargin()

        assertContentEquals(longArrayOf(133_749_546_712_057 + 152_441_447, 5_756_963_243_308 + 10), parseDiskBusyNanos(output))
        assertNull(parseDiskBusyNanos(""))
    }
}
