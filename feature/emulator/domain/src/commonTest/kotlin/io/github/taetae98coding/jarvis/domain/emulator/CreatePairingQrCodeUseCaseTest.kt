package io.github.taetae98coding.jarvis.domain.emulator

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CreatePairingQrCodeUseCaseTest {
    @Test
    fun payloadIsTheFormatAndroidSettingsReads() {
        val qr = CreatePairingQrCodeUseCase(Random(0))()

        assertEquals("WIFI:T:ADB;S:${qr.serviceName};P:${qr.password};;", qr.payload)
    }

    @Test
    fun nameAndPasswordNeedNoEscaping() {
        val qr = CreatePairingQrCodeUseCase(Random(0))()

        assertTrue(Regex("jarvis-[A-Za-z0-9]{10}").matches(qr.serviceName), qr.serviceName)
        assertTrue(Regex("[A-Za-z0-9]{12}").matches(qr.password), qr.password)
    }

    @Test
    fun everySessionIsNew() {
        val create = CreatePairingQrCodeUseCase(UuidRandom)

        assertNotEquals(create(), create())
    }
}
