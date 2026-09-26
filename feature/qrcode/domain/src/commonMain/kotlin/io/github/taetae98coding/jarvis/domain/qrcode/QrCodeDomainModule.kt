package io.github.taetae98coding.jarvis.domain.qrcode

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val qrCodeDomainModule = module {
    factoryOf(::ObserveQrCodeInputUseCase)
    factoryOf(::SelectQrContentTypeUseCase)
    factoryOf(::SetQrFieldUseCase)
    factoryOf(::SetQrErrorCorrectionUseCase)
    factoryOf(::GenerateQrCodeUseCase)
}
