package io.github.taetae98coding.jarvis.domain.emulator

import kotlin.random.Random

class CreatePairingQrCodeUseCase(
    private val random: Random,
) {
    operator fun invoke(): PairingQrCode =
        PairingQrCode(
            serviceName = ServiceNamePrefix + randomAlphanumeric(ServiceNameRandomLength),
            password = randomAlphanumeric(PasswordLength),
        )

    private fun randomAlphanumeric(length: Int): String =
        CharArray(length) { Alphanumeric[random.nextInt(Alphanumeric.length)] }.concatToString()

    private companion object {
        // mDNS 목록에서 누가 만든 세션인지 사람이 알아보게 붙인다. Android Studio 는 `studio-` 를 쓴다.
        const val ServiceNamePrefix = "jarvis-"
        const val ServiceNameRandomLength = 10
        const val PasswordLength = 12
        const val Alphanumeric = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }
}
