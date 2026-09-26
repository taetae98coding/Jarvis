package io.github.taetae98coding.jarvis.data.qrcode

import io.github.taetae98coding.jarvis.data.settings.SettingsPollInterval
import io.github.taetae98coding.jarvis.data.settings.SettingsStore
import io.github.taetae98coding.jarvis.data.state.observeSystemState
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeInput
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeSettingsRepository
import io.github.taetae98coding.jarvis.domain.qrcode.QrContentType
import io.github.taetae98coding.jarvis.domain.qrcode.QrErrorCorrection
import io.github.taetae98coding.jarvis.domain.qrcode.QrField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal class DefaultQrCodeSettingsRepository(
    private val store: SettingsStore,
) : QrCodeSettingsRepository {
    // 저장하지 않는 칸(Wi-Fi 비밀번호). single 이라 앱이 떠 있는 동안 카드와 화면이 같은 값을 본다.
    private val memory = MutableStateFlow(emptyMap<QrField, String>())

    // 칸이 열 개가 넘어 키마다 리스너를 붙이지 않고, 변경 신호 하나에 전부 다시 읽는다.
    override fun observeInput(): Flow<QrCodeInput> =
        combine(observeSystemState(signals = store.changes, interval = SettingsPollInterval, read = ::readPersisted), memory) { persisted, memory -> persisted.with(memory) }

    override fun readInput(): QrCodeInput = readPersisted().with(memory.value)

    override fun setContentType(type: QrContentType) {
        store.putString(TypeKey, type.storedValue)
    }

    override fun setField(field: QrField, value: String) {
        if (field.persisted) {
            store.putString(fieldKey(field), value)
        } else {
            memory.value += field to value
        }
    }

    override fun setErrorCorrection(errorCorrection: QrErrorCorrection) {
        store.putString(ErrorCorrectionKey, errorCorrection.storedValue)
    }

    private fun readPersisted(): QrCodeInput = QrCodeInput(
        type = QrContentType.fromStored(store.getString(TypeKey, QrContentType.TEXT.storedValue)),
        fields = QrField.entries.filter { it.persisted }.associateWith { store.getString(fieldKey(it), "") }.filterValues { it.isNotEmpty() },
        errorCorrection = QrErrorCorrection.fromStored(store.getString(ErrorCorrectionKey, QrErrorCorrection.Default.storedValue)),
    )

    private fun QrCodeInput.with(memory: Map<QrField, String>): QrCodeInput =
        copy(fields = fields + memory.filterValues { it.isNotEmpty() })

    internal companion object {
        const val TypeKey = "qrcode_type"
        const val ErrorCorrectionKey = "qrcode_error_correction"

        fun fieldKey(field: QrField): String = "qrcode_field_${field.storedValue}"
    }
}
