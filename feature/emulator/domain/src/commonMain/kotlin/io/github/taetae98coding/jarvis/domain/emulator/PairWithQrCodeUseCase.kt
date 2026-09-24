package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * QR 을 보여 주는 동안 한 번 수집한다. 그 이름의 서비스가 처음 보이면 목록을 그만 보고 그
 * 서비스와 페어링한 뒤 끝난다. 수집을 취소하면 기다림도 끝난다.
 */
class PairWithQrCodeUseCase(
    private val repository: DevicePairingRepository,
) {
    operator fun invoke(qr: PairingQrCode): Flow<QrPairingState> =
        flow {
            val service = repository.observePairingServices()
                .onEach { services -> emit(if (services == null) QrPairingState.Unavailable else QrPairingState.Waiting) }
                .mapNotNull { services -> services?.firstOrNull { it.name == qr.serviceName } }
                .first()

            emit(QrPairingState.Pairing(service))
            emit(QrPairingState.Finished(repository.pair(service, qr.password)))
        }.distinctUntilChanged()
}
