package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository
import org.koin.dsl.module

val appInfoDataModule = module {
    single<AppInfoRepository> { DefaultAppInfoRepository() }
}
