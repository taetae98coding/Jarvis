package io.github.taetae98coding.jarvis.domain.profiling

/** 이 앱이 돌고 있는 기기의 지표 한 벌. 줄 수 없는 지표는 [Reading.Unavailable] 이다. */
data class Profiling(
    val cpu: Reading<Usage>,
    val memory: Reading<MemoryUsage>,
    val gpu: Reading<Usage>,
    val network: Reading<NetworkThroughput>,
    val diskActivity: Reading<DiskActivity>,
    val diskSpace: Reading<DiskSpace>,
) {
    companion object {
        /** 첫 표본이 오기 전의 값. 지원하는 지표는 측정 중, 나머지는 첫 프레임부터 측정 불가다. */
        fun initial(supported: Set<ProfilingMetric>): Profiling {
            fun <T> reading(metric: ProfilingMetric): Reading<T> =
                if (metric in supported) Reading.Measuring else Reading.Unavailable

            return Profiling(
                cpu = reading(ProfilingMetric.CPU),
                memory = reading(ProfilingMetric.MEMORY),
                gpu = reading(ProfilingMetric.GPU),
                network = reading(ProfilingMetric.NETWORK),
                diskActivity = reading(ProfilingMetric.DISK_ACTIVITY),
                diskSpace = reading(ProfilingMetric.DISK_SPACE),
            )
        }
    }
}

enum class ProfilingMetric {
    CPU,
    MEMORY,
    GPU,
    NETWORK,
    DISK_ACTIVITY,
    DISK_SPACE,
}

sealed interface Reading<out T> {
    /** 차이로 구하는 지표의 첫 표본처럼, 값이 곧 온다. */
    data object Measuring : Reading<Nothing>

    /** 플랫폼이 줄 수 없거나 읽기에 실패했다. */
    data object Unavailable : Reading<Nothing>

    data class Available<out T>(val value: T) : Reading<T>
}

/** 기기 전체를 잰 값인지, 이 앱(Web 은 이 사이트)만 잰 값인지. */
enum class ProfilingScope {
    DEVICE,
    APP,
}

data class Usage(
    val percent: Double,
    val scope: ProfilingScope = ProfilingScope.DEVICE,
)

data class MemoryUsage(
    val usedBytes: Long,
    val totalBytes: Long,
    val scope: ProfilingScope = ProfilingScope.DEVICE,
)

data class NetworkThroughput(
    val receivedBytesPerSecond: Long,
    val sentBytesPerSecond: Long,
)

/** 표본 간격 동안 디스크가 읽기·쓰기 요청을 처리하던 시간의 비율. */
data class DiskActivity(
    val readPercent: Double,
    val writePercent: Double,
)

data class DiskSpace(
    val freeBytes: Long,
    val totalBytes: Long,
    val scope: ProfilingScope = ProfilingScope.DEVICE,
)
