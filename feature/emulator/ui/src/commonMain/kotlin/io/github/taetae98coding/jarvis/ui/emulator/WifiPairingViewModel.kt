package io.github.taetae98coding.jarvis.ui.emulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.emulator.CreatePairingQrCodeUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObservePairingServicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.PairWithCodeUseCase
import io.github.taetae98coding.jarvis.domain.emulator.PairWithQrCodeUseCase
import io.github.taetae98coding.jarvis.domain.emulator.PairingCodeLength
import io.github.taetae98coding.jarvis.domain.emulator.PairingQrCode
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import io.github.taetae98coding.jarvis.domain.emulator.QrPairingState
import io.github.taetae98coding.jarvis.domain.emulator.isValidPairingCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class WifiPairingTab {
    QrCode,
    PairingCode,
}

/** 코드 탭의 입력값과 진행 중인 요청. 줄은 서비스 이름으로 가린다. */
internal data class CodePairingState(
    val codes: Map<String, String> = emptyMap(),
    val pairing: Set<String> = emptySet(),
    val lastResult: CodePairingResult? = null,
) {
    fun codeOf(service: PairingService): String = codes[service.name].orEmpty()

    fun isPairing(service: PairingService): Boolean = service.name in pairing

    fun canPair(service: PairingService): Boolean = !isPairing(service) && isValidPairingCode(codeOf(service))
}

/**
 * 결과는 줄이 아니라 탭에 붙인다. 페어링에 성공하면 기기가 페어링 창을 닫아 그 줄이 목록에서
 * 사라지므로, 줄에 붙이면 결과를 읽기도 전에 없어진다.
 */
internal data class CodePairingResult(
    val service: PairingService,
    val result: PairingResult,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class WifiPairingViewModel(
    private val createPairingQrCode: CreatePairingQrCodeUseCase,
    pairWithQrCode: PairWithQrCodeUseCase,
    observePairingServices: ObservePairingServicesUseCase,
    private val pairWithCode: PairWithCodeUseCase,
) : ViewModel() {
    private val _tab = MutableStateFlow(WifiPairingTab.QrCode)
    val tab: StateFlow<WifiPairingTab> = _tab.asStateFlow()

    private val _qrCode = MutableStateFlow(createPairingQrCode())
    val qrCode: StateFlow<PairingQrCode> = _qrCode.asStateFlow()

    // 코드 탭을 보는 동안에도 QR 세션을 이어 간다. 구독에 묶으면 탭을 오갈 때마다 결과가 지워지고,
    // 이미 끝난 세션이 다시 스캔을 기다린다. 화면이 백스택에서 빠지면 viewModelScope 와 함께 끝난다.
    val qrState: StateFlow<QrPairingState> =
        _qrCode.flatMapLatest(pairWithQrCode::invoke)
            .stateIn(viewModelScope, SharingStarted.Eagerly, QrPairingState.Waiting)

    val services: StateFlow<List<PairingService>?> =
        observePairingServices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _codePairing = MutableStateFlow(CodePairingState())
    val codePairing: StateFlow<CodePairingState> = _codePairing.asStateFlow()

    fun onSelectTab(tab: WifiPairingTab) {
        _tab.value = tab
    }

    fun onNewQrCode() {
        _qrCode.value = createPairingQrCode()
    }

    fun onCodeChange(service: PairingService, code: String) {
        val digits = code.filter { it in '0'..'9' }.take(PairingCodeLength)

        _codePairing.update { it.copy(codes = it.codes + (service.name to digits)) }
    }

    fun onPair(service: PairingService) {
        val state = _codePairing.value
        if (!state.canPair(service)) return

        val code = state.codeOf(service)
        _codePairing.update { it.copy(pairing = it.pairing + service.name) }

        viewModelScope.launch {
            try {
                val result = pairWithCode(service, code)

                _codePairing.update { it.copy(lastResult = CodePairingResult(service, result)) }
            } finally {
                _codePairing.update { it.copy(pairing = it.pairing - service.name) }
            }
        }
    }
}
