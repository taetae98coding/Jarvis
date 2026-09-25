package io.github.taetae98coding.jarvis.data.profiling

/*
 * macOS 명령 출력을 숫자로 바꾼다. 출력 형태와 고른 키의 이유는 docs/platform/jvm.html#profiling 에 있다.
 * 형태가 맞지 않으면 null 이고, 그 줄은 측정 불가가 된다.
 */

/** 활동 모니터의 "사용된 메모리" 와 같은 식이다. */
internal fun parseVmStatUsedBytes(output: String): Long? {
    val pageSize = VmStatPageSize.find(output)?.groupValues?.get(1)?.toLongOrNull() ?: return null
    val pages = VmStatLine.findAll(output).associate { it.groupValues[1] to it.groupValues[2].toLong() }

    val anonymous = pages["Anonymous pages"] ?: return null
    val purgeable = pages["Pages purgeable"] ?: return null
    val wired = pages["Pages wired down"] ?: return null
    val compressor = pages["Pages occupied by compressor"] ?: return null

    return ((anonymous - purgeable).coerceAtLeast(0) + wired + compressor) * pageSize
}

/** GPU 가 여럿이면 가장 바쁜 것. */
internal fun parseGpuUtilization(output: String): Double? =
    GpuUtilization.findAll(output).maxOfOrNull { it.groupValues[1].toDouble() }

/** 루프백을 뺀 모든 인터페이스의 누적 `[받은 바이트, 보낸 바이트]`. */
internal fun parseNetstatBytes(output: String): LongArray? {
    val lines = output.lines()
    if (lines.firstOrNull()?.startsWith("Name") != true) return null

    var received = 0L
    var sent = 0L

    lines.drop(1).forEach { line ->
        val columns = line.trim().split(Whitespace)
        // 인터페이스마다 <Link#N> 줄이 하나씩 있다. 주소별 줄은 같은 카운터를 되풀이하므로 세지 않는다.
        if (columns.size < 10 || !columns[2].startsWith("<Link#") || columns[0].startsWith("lo")) return@forEach
        // 주소 열이 비는 인터페이스(utun 등)가 있어서 끝에서 센다: … Ibytes Opkts Oerrs Obytes Coll
        received += columns[columns.size - 5].toLongOrNull() ?: return@forEach
        sent += columns[columns.size - 2].toLongOrNull() ?: 0
    }

    return longArrayOf(received, sent)
}

/** 모든 블록 저장 장치가 읽기·쓰기에 쓴 누적 `[읽기 나노초, 쓰기 나노초]`. */
internal fun parseDiskBusyNanos(output: String): LongArray? {
    val read = DiskReadTime.findAll(output).map { it.groupValues[1].toLong() }.toList()
    val write = DiskWriteTime.findAll(output).map { it.groupValues[1].toLong() }.toList()
    if (read.isEmpty() || write.isEmpty()) return null

    return longArrayOf(read.sum(), write.sum())
}

private val VmStatPageSize = Regex("""page size of (\d+) bytes""")
private val VmStatLine = Regex("""^"?([^":]+)"?:\s+(\d+)\.?$""", RegexOption.MULTILINE)
private val GpuUtilization = Regex(""""Device Utilization %"=(\d+)""")
private val DiskReadTime = Regex(""""Total Time \(Read\)"=(\d+)""")
private val DiskWriteTime = Regex(""""Total Time \(Write\)"=(\d+)""")
private val Whitespace = Regex("""\s+""")
