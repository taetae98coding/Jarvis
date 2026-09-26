package io.github.taetae98coding.jarvis.domain.texttools

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** [SecureRandomSource] 는 textToolsDataModule 이 플랫폼 CSPRNG 로 준다. */
val textToolsDomainModule = module {
    factoryOf(::ObserveTextToolsSettingsUseCase)
    factoryOf(::SelectTextToolUseCase)
    factoryOf(::SetTextInputUseCase)
    factoryOf(::SetTextLimitUseCase)
    factoryOf(::SetPasswordOptionsUseCase)
    factoryOf(::PasswordGenerator)
    factoryOf(::GeneratePasswordUseCase)
    factoryOf(::CountTextUseCase)
    factoryOf(::TransformTextUseCase)
}
