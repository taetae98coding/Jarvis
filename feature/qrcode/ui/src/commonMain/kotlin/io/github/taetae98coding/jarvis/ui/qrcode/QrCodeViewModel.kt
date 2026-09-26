package io.github.taetae98coding.jarvis.ui.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.qrcode.GenerateQrCodeUseCase
import io.github.taetae98coding.jarvis.domain.qrcode.ObserveQrCodeInputUseCase
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeInput
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeResult
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import io.github.taetae98coding.jarvis.domain.qrcode.SelectQrContentTypeUseCase
import io.github.taetae98coding.jarvis.domain.qrcode.SetQrErrorCorrectionUseCase
import io.github.taetae98coding.jarvis.domain.qrcode.SetQrFieldUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class QrCodeViewModel(
    observeInput: ObserveQrCodeInputUseCase,
    private val selectType: SelectQrContentTypeUseCase,
    private val setField: SetQrFieldUseCase,
    private val setErrorCorrection: SetQrErrorCorrectionUseCase,
    private val generate: GenerateQrCodeUseCase,
) : ViewModel() {
    // 저장이 신호로 돌아오기 전에도 코드가 입력을 따라가도록, 화면이 마지막으로 넘긴 칸 값을 든다.
    private val drafts = MutableStateFlow(emptyMap<QrField, String>())

    val input: StateFlow<QrCodeInput> =
        combine(observeInput(), drafts, ::merge)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), merge(observeInput.initial(), drafts.value))

    val result: StateFlow<QrCodeResult> =
        input.map(generate::invoke)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), generate(input.value))

    /** 입력 칸을 처음 채울 값. 종류를 오가도 방금 친 글자가 저장값보다 앞선다. */
    fun fieldOf(field: QrField): String = input.value[field]

    fun onSelectType(type: QrContentType) {
        selectType(type)
    }

    fun onFieldChange(field: QrField, value: String) {
        drafts.update { it + (field to value) }
        setField(field, value)
    }

    fun onErrorCorrectionChange(errorCorrection: QrErrorCorrection) {
        setErrorCorrection(errorCorrection)
    }

    private fun merge(input: QrCodeInput, drafts: Map<QrField, String>): QrCodeInput = input.copy(fields = input.fields + drafts)
}

internal class QrCodeCardViewModel(
    observeInput: ObserveQrCodeInputUseCase,
    generate: GenerateQrCodeUseCase,
) : ViewModel() {
    val result: StateFlow<QrCodeResult> =
        observeInput().map(generate::invoke)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), generate(observeInput.initial()))
}
