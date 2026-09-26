package io.github.taetae98coding.jarvis.domain.qrcode

import kotlinx.coroutines.flow.Flow

/**
 * 마지막에 고른 종류, 칸마다의 값, 오류 정정 단계. [QrField.persisted] 인 칸은 앱을 껐다 켜도 남는다.
 *
 * [observeInput] 은 cold 라서 수집하는 동안에만 저장소 리스너가 붙는다. [readInput] 은 첫 프레임에 쓸 초기값이다.
 */
interface QrCodeSettingsRepository {
    fun observeInput(): Flow<QrCodeInput>

    fun readInput(): QrCodeInput

    fun setContentType(type: QrContentType)

    fun setField(field: QrField, value: String)

    fun setErrorCorrection(errorCorrection: QrErrorCorrection)
}
