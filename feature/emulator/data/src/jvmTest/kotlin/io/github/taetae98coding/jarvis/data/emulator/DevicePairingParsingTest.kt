package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DevicePairingParsingTest {
    // adb 37.0.1 의 실제 출력에 페어링 대기 줄 둘을 더했다.
    @Test
    fun readsOnlyPairingServices() {
        val output = """
            List of discovered mdns services
            adb-R54T202XEHN-Y2yH0N (2)	_adb-tls-connect._tcp	172.30.1.47:46465
            adb-R3CY705Y62R-WbpIT6	_adb-tls-pairing._tcp	172.30.1.87:37099
            jarvis-abcdefghij	_adb-tls-pairing._tcp	172.30.1.39:41234

        """.trimIndent()

        assertEquals(
            listOf(
                PairingService(name = "adb-R3CY705Y62R-WbpIT6", host = "172.30.1.87", port = 37099),
                PairingService(name = "jarvis-abcdefghij", host = "172.30.1.39", port = 41234),
            ),
            parsePairingServices(output),
        )
    }

    // 옛 adb 는 종류 끝에 점을 붙였고, 이름이 겹치면 공백 든 접미사가 붙는다.
    @Test
    fun toleratesOlderFormatsAndSpacesInNames() {
        val output = "adb-R3CY705Y62R-WbpIT6 (2)\t_adb-tls-pairing._tcp.\t172.30.1.87:37099\n"

        assertEquals(
            listOf(PairingService(name = "adb-R3CY705Y62R-WbpIT6 (2)", host = "172.30.1.87", port = 37099)),
            parsePairingServices(output),
        )
    }

    @Test
    fun readsTheGuidOfASuccessfulPair() {
        val output = "Successfully paired to 172.30.1.87:37099 [guid=adb-R3CY705Y62R-WbpIT6]\n"

        assertEquals("adb-R3CY705Y62R-WbpIT6", parsePairedGuid(output))
    }

    // android-14 까지는 실패해도 종료 코드가 0 이라, 성공 줄이 없는 출력은 실패로 읽어야 한다.
    @Test
    fun failuresAreNotPairs() {
        assertNull(parsePairedGuid("Failed: Wrong password or connection was dropped.\n"))
    }

    @Test
    fun failureReasonDropsTheErrorPrefix() {
        assertEquals(
            "Failed: Wrong password or connection was dropped.",
            parsePairFailure("error: Failed: Wrong password or connection was dropped.\n"),
        )
        assertEquals(
            "Failed: Wrong password or connection was dropped.",
            parsePairFailure("Failed: Wrong password or connection was dropped.\n"),
        )
    }
}
