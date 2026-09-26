package io.github.taetae98coding.jarvis.ui.profiling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.component.JarvisLabeledValue
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.profiling.DiskSpace
import io.github.taetae98coding.jarvis.domain.profiling.MemoryUsage
import io.github.taetae98coding.jarvis.domain.profiling.Profiling
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingMetric
import io.github.taetae98coding.jarvis.domain.profiling.ProfilingScope
import io.github.taetae98coding.jarvis.domain.profiling.Reading
import io.github.taetae98coding.jarvis.domain.profiling.Usage
import org.koin.compose.viewmodel.koinViewModel

const val ProfilingTestTag = "feature:profiling"

fun profilingRowTestTag(metric: ProfilingMetric): String =
    "$ProfilingTestTag:" + when (metric) {
        ProfilingMetric.CPU -> "cpu"
        ProfilingMetric.MEMORY -> "memory"
        ProfilingMetric.GPU -> "gpu"
        ProfilingMetric.NETWORK -> "network"
        ProfilingMetric.DISK_ACTIVITY -> "diskActivity"
        ProfilingMetric.DISK_SPACE -> "diskSpace"
    }

const val ProfilingMeasuring = "측정 중"
const val ProfilingUnavailable = "측정 불가"

@Composable
fun ProfilingCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<ProfilingViewModel>()
    val profiling by viewModel.profiling.collectAsStateWithLifecycle()

    ProfilingCard(profiling = profiling, modifier = modifier.testTag(ProfilingTestTag))
}

@Composable
internal fun ProfilingCard(
    profiling: Profiling,
    modifier: Modifier = Modifier,
) {
    JarvisCard(modifier = modifier) {
        JarvisCardHeader(title = ProfilingTitle, icon = JarvisIcons.Activity)

        MetricRow(ProfilingMetric.CPU, "CPU", profiling.cpu, scope = { it.scope }, fraction = Usage::fraction) {
            formatPercent(it.percent)
        }
        MetricRow(ProfilingMetric.MEMORY, "메모리", profiling.memory, scope = { it.scope }, fraction = MemoryUsage::fraction) {
            "${formatBytes(it.usedBytes)} / ${formatBytes(it.totalBytes)} (${formatPercent(it.fraction * 100.0)})"
        }
        MetricRow(ProfilingMetric.GPU, "GPU", profiling.gpu, scope = { it.scope }, fraction = Usage::fraction) {
            formatPercent(it.percent)
        }
        MetricRow(ProfilingMetric.NETWORK, "네트워크", profiling.network) {
            "↓ ${formatBytesPerSecond(it.receivedBytesPerSecond)} · ↑ ${formatBytesPerSecond(it.sentBytesPerSecond)}"
        }
        MetricRow(ProfilingMetric.DISK_ACTIVITY, "디스크 읽기·쓰기", profiling.diskActivity) {
            "읽기 ${formatPercent(it.readPercent)} · 쓰기 ${formatPercent(it.writePercent)}"
        }
        // 막대는 남은 양이 아니라 쓴 양이다. 다른 막대와 같이 길수록 여유가 없다.
        MetricRow(
            ProfilingMetric.DISK_SPACE,
            "디스크 남은 용량",
            profiling.diskSpace,
            scope = { it.scope },
            fraction = DiskSpace::usedFraction,
        ) {
            "${formatBytes(it.freeBytes)} / ${formatBytes(it.totalBytes)}"
        }
    }
}

@Composable
private fun <T> MetricRow(
    metric: ProfilingMetric,
    label: String,
    reading: Reading<T>,
    scope: (T) -> ProfilingScope = { ProfilingScope.DEVICE },
    fraction: ((T) -> Float)? = null,
    format: (T) -> String,
) {
    val value = (reading as? Reading.Available)?.value
    val text = when (reading) {
        Reading.Measuring -> ProfilingMeasuring
        Reading.Unavailable -> ProfilingUnavailable
        is Reading.Available -> format(reading.value)
    }
    val scopedLabel = if (value != null && scope(value) == ProfilingScope.APP) "$label (이 앱)" else label

    Column(
        modifier = Modifier
            .testTag(profilingRowTestTag(metric))
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        JarvisLabeledValue(label = scopedLabel, value = text)

        if (value != null && fraction != null) {
            LinearProgressIndicator(
                progress = { fraction(value).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val Usage.fraction: Float get() = (percent / 100.0).toFloat()

private val MemoryUsage.fraction: Float get() = if (totalBytes <= 0) 0f else usedBytes.toFloat() / totalBytes

private val DiskSpace.usedFraction: Float get() = if (totalBytes <= 0) 0f else 1f - freeBytes.toFloat() / totalBytes
