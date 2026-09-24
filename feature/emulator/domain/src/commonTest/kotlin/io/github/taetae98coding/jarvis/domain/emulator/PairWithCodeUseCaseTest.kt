package io.github.taetae98coding.jarvis.domain.emulator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PairWithCodeUseCaseTest {
    @Test
    fun sendsSixDigitCodes() = runTest {
        val repository = FakeDevicePairingRepository()

        val result = PairWithCodeUseCase(repository)(CodeService, "012345")

        assertEquals(listOf(CodeService to "012345"), repository.paired)
        assertEquals(PairingResult.Paired(isConnected = true), result)
    }

    // 틀린 것이 확실한 코드를 보내면 기기가 페어링 창을 닫아 버려서 사용자가 다시 열어야 한다.
    @Test
    fun keepsOtherCodesFromTheDevice() = runTest {
        val repository = FakeDevicePairingRepository()

        listOf("", "12345", "1234567", "12a456", "１２３４５６").forEach { code ->
            assertIs<PairingResult.Failed>(PairWithCodeUseCase(repository)(CodeService, code), code)
        }
        assertEquals(emptyList(), repository.paired)
    }
}
