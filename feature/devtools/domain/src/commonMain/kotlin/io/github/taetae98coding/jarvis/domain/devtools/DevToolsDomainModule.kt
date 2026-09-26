package io.github.taetae98coding.jarvis.domain.devtools

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val devToolsDomainModule = module {
    factoryOf(::ObserveDevToolStateUseCase)
    factoryOf(::SelectDevToolUseCase)
    factoryOf(::SetDevToolInputUseCase)
    factoryOf(::ConvertDevToolInputUseCase)
    // 생성자의 기본값(무작위·시스템 시계)을 쓴다. factoryOf 는 기본값이 있는 인자도 Koin 에서 찾으려 한다.
    factory { GenerateUuidsUseCase() }
    factory { GetCurrentEpochSecondsUseCase() }
}
