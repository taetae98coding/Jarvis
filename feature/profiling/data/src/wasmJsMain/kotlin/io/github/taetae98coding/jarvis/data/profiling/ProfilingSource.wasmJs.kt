package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.profiling.DiskSpace
import io.github.taetae98coding.jarvis.domain.profiling.MemoryUsage
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingScope
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import kotlinx.coroutines.await
import kotlin.js.Promise

internal actual fun createProfilingSource(context: PlatformContext): ProfilingSource = BrowserProfilingSource

// 브라우저는 기기 지표를 주지 않는다. 이 탭과 이 사이트에 관한 두 가지만 있다(docs/platform/web.html#profiling).
private object BrowserProfilingSource : ProfilingSource {
    override val supportedMetrics: Set<ProfilingMetric> = setOf(ProfilingMetric.MEMORY, ProfilingMetric.DISK_SPACE)

    override fun openSampler(): ProfilingSampler = ProfilingSampler {
        Profiling(
            cpu = Reading.Unavailable,
            memory = readJsHeap(),
            gpu = Reading.Unavailable,
            network = Reading.Unavailable,
            diskActivity = Reading.Unavailable,
            diskSpace = readStorageEstimate(),
        )
    }

    private fun readJsHeap(): Reading<MemoryUsage> {
        val used = jsHeapUsedBytes()
        val limit = jsHeapLimitBytes()
        if (used < 0 || limit <= 0) return Reading.Unavailable

        return Reading.Available(MemoryUsage(usedBytes = used.toLong(), totalBytes = limit.toLong(), scope = ProfilingScope.APP))
    }

    private suspend fun readStorageEstimate(): Reading<DiskSpace> {
        val estimate = runCatching { storageEstimate()?.await<JsAny>() }.getOrNull() ?: return Reading.Unavailable
        val quota = estimateQuota(estimate)
        if (quota <= 0) return Reading.Unavailable

        val free = (quota - estimateUsage(estimate)).coerceAtLeast(0.0)
        return Reading.Available(DiskSpace(freeBytes = free.toLong(), totalBytes = quota.toLong(), scope = ProfilingScope.APP))
    }
}

// performance.memory 는 Chromium 에만 있는 비표준 속성이다. 없으면 -1.
private fun jsHeapUsedBytes(): Double = js("(performance.memory ? performance.memory.usedJSHeapSize : -1)")

private fun jsHeapLimitBytes(): Double = js("(performance.memory ? performance.memory.jsHeapSizeLimit : -1)")

// navigator.storage 는 보안 컨텍스트(HTTPS·localhost)에만 있다.
private fun storageEstimate(): Promise<JsAny>? =
    js("((navigator.storage && navigator.storage.estimate) ? navigator.storage.estimate() : null)")

private fun estimateQuota(estimate: JsAny): Double = js("(estimate.quota || 0)")

private fun estimateUsage(estimate: JsAny): Double = js("(estimate.usage || 0)")
