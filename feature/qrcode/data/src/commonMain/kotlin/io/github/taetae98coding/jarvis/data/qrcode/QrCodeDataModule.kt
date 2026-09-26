package io.github.taetae98coding.jarvis.data.qrcode

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeSettingsRepository
import org.koin.dsl.module

val qrCodeDataModule = module {
    single<QrCodeSettingsRepository> { DefaultQrCodeSettingsRepository(createSettingsStore(get())) }
}
