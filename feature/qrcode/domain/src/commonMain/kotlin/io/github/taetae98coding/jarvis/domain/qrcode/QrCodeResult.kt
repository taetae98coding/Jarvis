package io.github.taetae98coding.jarvis.domain.qrcode

sealed interface QrCodeResult {
    data object Empty : QrCodeResult

    data class Missing(val field: QrField) : QrCodeResult

    data class TooLong(val payload: String, val mode: QrMode, val limit: Int, val length: Int) : QrCodeResult

    data class Ready(val payload: String, val code: QrCode) : QrCodeResult
}

class GenerateQrCodeUseCase {
    operator fun invoke(input: QrCodeInput): QrCodeResult =
        when (val payload = QrPayload.build(input)) {
            QrPayloadResult.Empty -> QrCodeResult.Empty
            is QrPayloadResult.Missing -> QrCodeResult.Missing(payload.field)
            is QrPayloadResult.Ready -> when (val encoded = QrEncoder.encode(payload.text, input.errorCorrection)) {
                is QrEncodeResult.Success -> QrCodeResult.Ready(payload.text, encoded.code)
                is QrEncodeResult.TooLong -> QrCodeResult.TooLong(payload.text, encoded.mode, encoded.limit, encoded.length)
            }
        }
}
