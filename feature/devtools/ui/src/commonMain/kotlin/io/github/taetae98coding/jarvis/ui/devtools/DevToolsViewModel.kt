package io.github.taetae98coding.jarvis.ui.devtools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.devtools.ConvertDevToolInputUseCase
import io.github.taetae98coding.jarvis.domain.devtools.DevTool
import io.github.taetae98coding.jarvis.domain.devtools.DevToolOutput
import io.github.taetae98coding.jarvis.domain.devtools.DevToolState
import io.github.taetae98coding.jarvis.domain.devtools.GenerateUuidsUseCase
import io.github.taetae98coding.jarvis.domain.devtools.GetCurrentEpochSecondsUseCase
import io.github.taetae98coding.jarvis.domain.devtools.ObserveDevToolStateUseCase
import io.github.taetae98coding.jarvis.domain.devtools.RgbColor
import io.github.taetae98coding.jarvis.domain.devtools.SelectDevToolUseCase
import io.github.taetae98coding.jarvis.domain.devtools.SetDevToolInputUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class DevToolsViewModel(
    observeState: ObserveDevToolStateUseCase,
    private val selectTool: SelectDevToolUseCase,
    private val setInput: SetDevToolInputUseCase,
    private val convert: ConvertDevToolInputUseCase,
    private val generateUuids: GenerateUuidsUseCase,
    private val currentEpochSeconds: GetCurrentEpochSecondsUseCase,
) : ViewModel() {
    val state: StateFlow<DevToolState> = observeState(viewModelScope)

    // 저장이 신호로 돌아오기 전에도 결과가 입력을 따라가도록, 도구마다 화면이 마지막으로 넘긴 입력을 든다.
    private val drafts = MutableStateFlow(emptyMap<DevTool, String>())

    private val _base64UrlSafe = MutableStateFlow(false)
    val base64UrlSafe: StateFlow<Boolean> = _base64UrlSafe.asStateFlow()

    private val _uuidCount = MutableStateFlow(UuidCounts.first())
    val uuidCount: StateFlow<Int> = _uuidCount.asStateFlow()

    private val _uuidUppercase = MutableStateFlow(false)
    val uuidUppercase: StateFlow<Boolean> = _uuidUppercase.asStateFlow()

    private val _uuids = MutableStateFlow(generateUuids(count = UuidCounts.first(), uppercase = false))
    val uuids: StateFlow<List<String>> = _uuids.asStateFlow()

    val results: StateFlow<DevToolResults> =
        combine(state, drafts, base64UrlSafe) { state, drafts, urlSafe -> results(state, drafts, urlSafe) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), results(state.value, drafts.value, base64UrlSafe.value))

    /** 입력 칸을 처음 채울 값. 도구를 오가도 방금 친 글자가 저장값보다 앞선다. */
    fun inputOf(state: DevToolState): String = drafts.value[state.tool] ?: state.input

    fun onSelectTool(tool: DevTool) {
        selectTool(tool)
    }

    fun onInputChange(tool: DevTool, input: String) {
        drafts.update { it + (tool to input) }
        setInput(tool, input)
    }

    fun currentEpochSecondsText(): String = currentEpochSeconds().toString()

    fun onBase64UrlSafeChange(urlSafe: Boolean) {
        _base64UrlSafe.value = urlSafe
    }

    fun onUuidCountChange(count: Int) {
        _uuidCount.value = count
    }

    fun onUuidUppercaseChange(uppercase: Boolean) {
        _uuidUppercase.value = uppercase
        _uuids.update { list -> list.map { if (uppercase) it.uppercase() else it.lowercase() } }
    }

    fun onGenerateUuids() {
        _uuids.value = generateUuids(count = uuidCount.value, uppercase = uuidUppercase.value)
    }

    private fun results(state: DevToolState, drafts: Map<DevTool, String>, urlSafe: Boolean): DevToolResults {
        val input = drafts[state.tool] ?: state.input

        return DevToolResults(
            outputs = convert(state.tool, input, urlSafe),
            swatch = convert.swatch(state.tool, input),
        )
    }
}

internal data class DevToolResults(
    val outputs: List<DevToolOutput>,
    val swatch: RgbColor?,
)

internal val UuidCounts = listOf(1, 5, 10)
