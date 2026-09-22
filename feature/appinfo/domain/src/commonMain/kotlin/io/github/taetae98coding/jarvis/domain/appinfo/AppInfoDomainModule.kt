package io.github.taetae98coding.jarvis.domain.appinfo

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appInfoDomainModule = module {
    factoryOf(::GetAppInfoUseCase)
}
