package io.github.taetae98coding.jarvis.ui.emulator

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.DeviceLogLine
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.ObserveDeviceLogUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObserveEmulatorDevicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.canReadLog
import io.github.taetae98coding.jarvis.domain.emulator.matches
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/** [id] 는 이 ViewModel 안에서 늘기만 하는 순번이다. 줄 목록의 key 와 "지우기" 의 기준으로 쓴다. */
@Immutable
internal data class DeviceLogEntry(
    val id: Long,
    val line: DeviceLogLine,
)

@Immutable
internal data class DeviceLogState(
    val isSupported: Boolean = true,
    val hasReceived: Boolean = false,
    val lines: List<DeviceLogEntry> = emptyList(),
)

/**
 * 기기 탭 로그 창 하나의 상태(docs/common/device-logcat.html). 수집하는 동안에만 기기 로그를 읽는다. 다시 수집하면 처음부터 새로 모은다.
 */
internal class DeviceLogViewModel(
    val deviceId: String,
    observeEmulatorDevices: ObserveEmulatorDevicesUseCase,
    observeDeviceLog: ObserveDeviceLogUseCase,
) : ViewModel() {
    private val device: StateFlow<EmulatorDevice?> = observeEmulatorDevices()
        .map { devices -> devices.firstOrNull { it.id == deviceId } ?: devices.firstOrNull { it.matches(deviceId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private var nextId = 0L

    // `avd:<이름>` 은 켜진 에뮬레이터의 시리얼로 풀릴 때마다 그 기기의 로그로 갈아탄다(R12). 목록이 오기 전에는 받은 id 그대로 읽는다.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val entries = device
        .map { it?.id ?: deviceId }
        .distinctUntilChanged()
        .flatMapLatest { observeDeviceLog(it) }
        .scan(emptyList<DeviceLogEntry>()) { entries, batch ->
            (entries + batch.map { DeviceLogEntry(nextId++, it) }).takeLast(MaxLines)
        }

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val clearedThrough = MutableStateFlow(-1L)

    val state: StateFlow<DeviceLogState> =
        combine(device.map { it?.canReadLog ?: true }, entries, _filter, clearedThrough) { isSupported, entries, filter, cleared ->
            val kept = entries.filter { it.id > cleared }

            DeviceLogState(
                isSupported = isSupported,
                hasReceived = kept.isNotEmpty(),
                lines = if (filter.isBlank()) kept else kept.filter { it.line.text.contains(filter, ignoreCase = true) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DeviceLogState())

    fun setFilter(filter: String) {
        _filter.value = filter
    }

    fun clear() {
        clearedThrough.value = nextId - 1
    }

    private companion object {
        const val MaxLines = 5000
    }
}
