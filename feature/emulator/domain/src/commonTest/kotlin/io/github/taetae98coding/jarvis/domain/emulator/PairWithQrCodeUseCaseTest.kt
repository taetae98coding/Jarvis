package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PairWithQrCodeUseCaseTest {
    private val qr = PairingQrCode(serviceName = "jarvis-abcdefghij", password = "Secret123456")

    private val scanned = PairingService(name = qr.serviceName, host = "172.30.1.47", port = 41234)

    @Test
    fun pairsWithTheScannedServiceUsingThePassword() = runTest {
        val repository = FakeDevicePairingRepository()
        val states = mutableListOf<QrPairingState>()
        backgroundScope.launch { PairWithQrCodeUseCase(repository)(qr).toList(states) }
        runCurrent()

        // 페어링 코드 창을 연 다른 기기에는 반응하지 않는다.
        repository.services.value = listOf(CodeService)
        runCurrent()
        repository.services.value = listOf(CodeService, scanned)
        runCurrent()

        assertEquals(listOf(scanned to qr.password), repository.paired)
        assertEquals(
            listOf(
                QrPairingState.Waiting,
                QrPairingState.Pairing(scanned),
                QrPairingState.Finished(PairingResult.Paired(isConnected = true)),
            ),
            states,
        )
    }

    @Test
    fun saysWhenNothingCanBeFound() = runTest {
        val repository = FakeDevicePairingRepository(services = null)
        val states = mutableListOf<QrPairingState>()
        backgroundScope.launch { PairWithQrCodeUseCase(repository)(qr).toList(states) }
        runCurrent()

        repository.services.value = emptyList()
        runCurrent()

        assertEquals(listOf(QrPairingState.Unavailable, QrPairingState.Waiting), states)
    }

    @Test
    fun failureIsTheLastState() = runTest {
        val failure = PairingResult.Failed("Failed: Wrong password or connection was dropped.")
        val repository = FakeDevicePairingRepository(services = listOf(scanned), result = failure)

        val states = PairWithQrCodeUseCase(repository)(qr).toList()

        assertEquals(QrPairingState.Finished(failure), states.last())
    }
}
