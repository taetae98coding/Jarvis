package io.github.taetae98coding.jarvis.ui.emulator

import io.github.taetae98coding.jarvis.domain.emulator.CreatePairingQrCodeUseCase
import io.github.taetae98coding.jarvis.domain.emulator.ObservePairingServicesUseCase
import io.github.taetae98coding.jarvis.domain.emulator.PairWithCodeUseCase
import io.github.taetae98coding.jarvis.domain.emulator.PairWithQrCodeUseCase
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import io.github.taetae98coding.jarvis.domain.emulator.QrPairingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WifiPairingViewModelTest {
    // EmulatorDevicesViewModelTest 와 같은 이유로 runTest 밖에서 되돌린다.
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun qrSessionPairsTheScannedDeviceWithItsPassword() {
        val pairing = FakeDevicePairingRepository()

        viewModelTest(pairing) { viewModel ->
            val qr = viewModel.qrCode.value
            val scanned = PairingService(name = qr.serviceName, host = "172.30.1.39", port = 41234)
            runCurrent()
            assertEquals(QrPairingState.Waiting, viewModel.qrState.value)

            pairing.services.value = listOf(WaitingPairingService, scanned)
            runCurrent()
            assertEquals(QrPairingState.Pairing(scanned), viewModel.qrState.value)

            pairing.result.complete(PairingResult.Paired(isConnected = true))
            runCurrent()
            assertEquals(QrPairingState.Finished(PairingResult.Paired(isConnected = true)), viewModel.qrState.value)
            assertEquals(listOf(scanned to qr.password), pairing.paired)
        }
    }

    // 끝난 세션의 결과는 탭을 오가도 남는다.
    @Test
    fun qrResultSurvivesSwitchingTabs() {
        val pairing = FakeDevicePairingRepository()

        viewModelTest(pairing) { viewModel ->
            pairing.services.value = listOf(PairingService(viewModel.qrCode.value.serviceName, "172.30.1.39", 41234))
            pairing.result.complete(PairingResult.Failed("Failed: Wrong password or connection was dropped."))
            runCurrent()

            viewModel.onSelectTab(WifiPairingTab.PairingCode)
            viewModel.onSelectTab(WifiPairingTab.QrCode)
            runCurrent()

            assertEquals(
                QrPairingState.Finished(PairingResult.Failed("Failed: Wrong password or connection was dropped.")),
                viewModel.qrState.value,
            )
        }
    }

    @Test
    fun newQrCodeStartsANewSession() {
        viewModelTest(FakeDevicePairingRepository()) { viewModel ->
            val first = viewModel.qrCode.value

            viewModel.onNewQrCode()
            runCurrent()

            assertNotEquals(first, viewModel.qrCode.value)
            assertEquals(QrPairingState.Waiting, viewModel.qrState.value)
        }
    }

    @Test
    fun codeFieldKeepsOnlySixDigits() {
        viewModelTest(FakeDevicePairingRepository()) { viewModel ->
            viewModel.onCodeChange(WaitingPairingService, "12a3 45678")

            assertEquals("123456", viewModel.codePairing.value.codeOf(WaitingPairingService))
        }
    }

    @Test
    fun pairingLocksTheRowUntilTheResultArrives() {
        val pairing = FakeDevicePairingRepository(services = listOf(WaitingPairingService))

        viewModelTest(pairing) { viewModel ->
            viewModel.onCodeChange(WaitingPairingService, "123456")
            viewModel.onPair(WaitingPairingService)
            viewModel.onPair(WaitingPairingService)
            runCurrent()

            assertTrue(viewModel.codePairing.value.isPairing(WaitingPairingService))
            assertFalse(viewModel.codePairing.value.canPair(WaitingPairingService))
            assertEquals(listOf(WaitingPairingService to "123456"), pairing.paired)

            pairing.result.complete(PairingResult.Paired(isConnected = true))
            runCurrent()

            assertFalse(viewModel.codePairing.value.isPairing(WaitingPairingService))
            assertEquals(
                CodePairingResult(WaitingPairingService, PairingResult.Paired(isConnected = true)),
                viewModel.codePairing.value.lastResult,
            )
        }
    }

    @Test
    fun incompleteCodesAreNotSent() {
        val pairing = FakeDevicePairingRepository(services = listOf(WaitingPairingService))

        viewModelTest(pairing) { viewModel ->
            viewModel.onCodeChange(WaitingPairingService, "12345")
            viewModel.onPair(WaitingPairingService)
            runCurrent()

            assertEquals(emptyList(), pairing.paired)
        }
    }

    private fun viewModelTest(
        pairing: FakeDevicePairingRepository,
        body: suspend TestScope.(WifiPairingViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = WifiPairingViewModel(
            createPairingQrCode = CreatePairingQrCodeUseCase(Random(0)),
            pairWithQrCode = PairWithQrCodeUseCase(pairing),
            observePairingServices = ObservePairingServicesUseCase(pairing),
            pairWithCode = PairWithCodeUseCase(pairing),
        )

        body(viewModel)
    }
}
