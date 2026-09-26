package io.github.taetae98coding.jarvis.domain.qrcode

import kotlinx.coroutines.flow.Flow

class ObserveQrCodeInputUseCase(
    private val settings: QrCodeSettingsRepository,
) {
    operator fun invoke(): Flow<QrCodeInput> = settings.observeInput()

    /** `stateIn` 의 초기값으로만 쓴다. */
    fun initial(): QrCodeInput = settings.readInput()
}

class SelectQrContentTypeUseCase(
    private val settings: QrCodeSettingsRepository,
) {
    operator fun invoke(type: QrContentType) {
        settings.setContentType(type)
    }
}

class SetQrFieldUseCase(
    private val settings: QrCodeSettingsRepository,
) {
    operator fun invoke(field: QrField, value: String) {
        settings.setField(field, value)
    }
}

class SetQrErrorCorrectionUseCase(
    private val settings: QrCodeSettingsRepository,
) {
    operator fun invoke(errorCorrection: QrErrorCorrection) {
        settings.setErrorCorrection(errorCorrection)
    }
}
